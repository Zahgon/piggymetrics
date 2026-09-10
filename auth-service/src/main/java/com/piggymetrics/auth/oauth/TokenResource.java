package com.piggymetrics.auth.oauth;

import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * RFC 6749 token endpoint.
 *
 * <p>was: Spring Cloud Security's {@code @EnableAuthorizationServer} auto-registered
 * {@code /oauth/token}. Supported grants are the ones the PiggyMetrics UI and the sibling
 * services actually use:</p>
 * <ul>
 *   <li>{@code password} - public client {@code browser}, scope {@code ui} only;</li>
 *   <li>{@code client_credentials} - the three service clients, scope {@code server}.</li>
 * </ul>
 *
 * <p>{@code refresh_token} was declared by the original client registry but is never
 * exercised by the UI (the front-end re-runs the password grant), so it is deliberately
 * NOT implemented; a request for it returns {@code unsupported_grant_type}.</p>
 *
 * <p>Tokens were opaque + {@code InMemoryTokenStore}; they are now self-contained RS256
 * JWTs verifiable through {@code /uaa/.well-known/jwks.json}.</p>
 */
@Path("/oauth/token")
public class TokenResource {

	/** 12h - Spring's DefaultTokenServices default access token validity. */
	private static final long EXPIRES_IN_SECONDS = 43200L;

	private final UserRepository userRepository;
	private final OAuthClients clients;
	private final JwkService jwkService;
	private final String issuer;

	@Inject
	public TokenResource(UserRepository userRepository, OAuthClients clients, JwkService jwkService,
			@ConfigProperty(name = "piggymetrics.jwt.issuer") String issuer) {
		this.userRepository = userRepository;
		this.clients = clients;
		this.jwkService = jwkService;
		this.issuer = issuer;
	}

	@POST
	@PermitAll
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response token(
			@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
			@FormParam("grant_type") String grantType,
			@FormParam("username") String username,
			@FormParam("password") String password,
			@FormParam("scope") String requestedScope,
			@FormParam("client_id") String formClientId,
			@FormParam("client_secret") String formClientSecret) {

		ClientCredentials credentials = ClientCredentials.parse(authorization)
				.orElseGet(() -> new ClientCredentials(formClientId, formClientSecret));

		if (credentials.clientId() == null || credentials.clientId().isBlank()) {
			return error(Response.Status.UNAUTHORIZED, "invalid_client", "client authentication is required");
		}

		if ("password".equals(grantType)) {
			return passwordGrant(credentials, username, password, requestedScope);
		}
		if ("client_credentials".equals(grantType)) {
			return clientCredentialsGrant(credentials, requestedScope);
		}
		return error(Response.Status.BAD_REQUEST, "unsupported_grant_type",
				"supported grant types are: password, client_credentials");
	}

	private Response passwordGrant(ClientCredentials credentials, String username, String password,
			String requestedScope) {

		// only the public `browser` client may run the password grant
		if (!clients.isBrowserClient(credentials.clientId())) {
			return error(Response.Status.UNAUTHORIZED, "invalid_client",
					"client is not allowed to use the password grant");
		}
		// `browser` is a public client registered without a secret - accept absent/empty only
		if (credentials.clientSecret() != null && !credentials.clientSecret().isEmpty()) {
			return error(Response.Status.UNAUTHORIZED, "invalid_client", "unknown client credentials");
		}
		// a UI user must never be able to mint a service-scope token
		if (!isScopeAllowed(requestedScope, OAuthClients.SCOPE_UI)) {
			return error(Response.Status.BAD_REQUEST, "invalid_scope",
					"client 'browser' is limited to the '" + OAuthClients.SCOPE_UI + "' scope");
		}
		if (username == null || username.isBlank() || password == null || password.isEmpty()) {
			return error(Response.Status.BAD_REQUEST, "invalid_request", "username and password are required");
		}

		Optional<User> user = userRepository.findByUsername(username);
		if (user.isEmpty() || !BcryptUtil.matches(password, user.get().getPassword())) {
			return error(Response.Status.BAD_REQUEST, "invalid_grant", "bad credentials");
		}

		return issue(user.get().getUsername(), credentials.clientId(), OAuthClients.SCOPE_UI);
	}

	private Response clientCredentialsGrant(ClientCredentials credentials, String requestedScope) {

		if (!clients.isServiceClient(credentials.clientId())
				|| !clients.matchesServiceClientSecret(credentials.clientId(), credentials.clientSecret())) {
			return error(Response.Status.UNAUTHORIZED, "invalid_client", "unknown client credentials");
		}
		if (!isScopeAllowed(requestedScope, OAuthClients.SCOPE_SERVER)) {
			return error(Response.Status.BAD_REQUEST, "invalid_scope",
					"service clients are limited to the '" + OAuthClients.SCOPE_SERVER + "' scope");
		}
		return issue(credentials.clientId(), credentials.clientId(), OAuthClients.SCOPE_SERVER);
	}

	/** An absent scope defaults to the client's only registered scope; anything else must match it. */
	private static boolean isScopeAllowed(String requestedScope, String allowedScope) {
		return requestedScope == null || requestedScope.isBlank() || allowedScope.equals(requestedScope.trim());
	}

	private Response issue(String name, String clientId, String scope) {
		String accessToken = Jwt.issuer(issuer)
				.subject(name)
				.upn(name)
				// MP-JWT `groups` mirrors the scope so @RolesAllowed("server") works locally,
				// while `scope` is what the sibling resource servers map onto roles.
				.groups(Set.of(scope))
				.claim("scope", scope)
				.claim("client_id", clientId)
				.expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS))
				.jws().keyId(jwkService.keyId())
				.sign();

		return Response.ok(Map.of(
				"access_token", accessToken,
				"token_type", "Bearer",
				"expires_in", EXPIRES_IN_SECONDS,
				"scope", scope)).build();
	}

	private static Response error(Response.Status status, String error, String description) {
		return Response.status(status)
				.entity(Map.of("error", error, "error_description", description))
				.type(MediaType.APPLICATION_JSON)
				.build();
	}

	/** HTTP Basic client authentication, parsed by hand: this endpoint is unauthenticated. */
	record ClientCredentials(String clientId, String clientSecret) {

		static Optional<ClientCredentials> parse(String authorizationHeader) {
			if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Basic ", 0, 6)) {
				return Optional.empty();
			}
			byte[] decoded;
			try {
				decoded = Base64.getDecoder().decode(authorizationHeader.substring(6).trim());
			}
			catch (IllegalArgumentException e) {
				return Optional.empty();
			}
			String value = new String(decoded, StandardCharsets.UTF_8);
			int separator = value.indexOf(':');
			if (separator < 0) {
				return Optional.of(new ClientCredentials(value, ""));
			}
			return Optional.of(new ClientCredentials(value.substring(0, separator), value.substring(separator + 1)));
		}
	}
}
