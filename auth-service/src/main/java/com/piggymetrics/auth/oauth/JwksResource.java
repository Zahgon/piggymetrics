package com.piggymetrics.auth.oauth;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * was: the Spring authorization server's {@code /oauth/token_key} with
 * {@code tokenKeyAccess("permitAll()")}. Now a standards JWKS document, since the sibling
 * services verify RS256 signatures instead of calling {@code /user} for token introspection.
 */
@Path("/.well-known/jwks.json")
public class JwksResource {

	private final JwkService jwkService;

	@Inject
	public JwksResource(JwkService jwkService) {
		this.jwkService = jwkService;
	}

	@GET
	@PermitAll
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> jwks() {
		return jwkService.jwks();
	}
}
