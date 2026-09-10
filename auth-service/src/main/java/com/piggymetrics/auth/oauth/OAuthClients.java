package com.piggymetrics.auth.oauth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.Optional;

/**
 * In-memory client registry.
 *
 * <p>was: {@code OAuth2AuthorizationConfig#configure(ClientDetailsServiceConfigurer)}.
 * Client secrets are compared as PLAINTEXT, exactly like the original which installed
 * {@code NoOpPasswordEncoder} on the authorization server. User passwords stay BCrypt.</p>
 */
@ApplicationScoped
public class OAuthClients {

	public static final String BROWSER_CLIENT_ID = "browser";

	public static final String SCOPE_UI = "ui";
	public static final String SCOPE_SERVER = "server";

	private final Map<String, Optional<String>> serviceClientSecrets;

	@Inject
	public OAuthClients(
			@ConfigProperty(name = "ACCOUNT_SERVICE_PASSWORD") Optional<String> accountServiceSecret,
			@ConfigProperty(name = "STATISTICS_SERVICE_PASSWORD") Optional<String> statisticsServiceSecret,
			@ConfigProperty(name = "NOTIFICATION_SERVICE_PASSWORD") Optional<String> notificationServiceSecret) {
		this.serviceClientSecrets = Map.of(
				"account-service", accountServiceSecret,
				"statistics-service", statisticsServiceSecret,
				"notification-service", notificationServiceSecret);
	}

	public boolean isBrowserClient(String clientId) {
		return BROWSER_CLIENT_ID.equals(clientId);
	}

	public boolean isServiceClient(String clientId) {
		return serviceClientSecrets.containsKey(clientId);
	}

	/**
	 * Plaintext secret comparison for the {@code client_credentials} service clients.
	 * A client whose secret is not configured (env var absent/blank) can never authenticate.
	 */
	public boolean matchesServiceClientSecret(String clientId, String providedSecret) {
		Optional<String> configured = serviceClientSecrets.getOrDefault(clientId, Optional.empty());
		if (configured.isEmpty() || configured.get().isBlank() || providedSecret == null) {
			return false;
		}
		return configured.get().equals(providedSecret);
	}
}
