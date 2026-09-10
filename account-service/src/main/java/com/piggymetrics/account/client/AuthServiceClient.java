package com.piggymetrics.account.client;

import com.piggymetrics.account.domain.User;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * {@code @OidcClientFilter} attaches a {@code Bearer} token obtained from the default
 * {@code quarkus.oidc-client} (account-service's own client_credentials, scope {@code server}).
 * This is the replacement for Spring Cloud Security's {@code OAuth2FeignRequestInterceptor}.
 */
@OidcClientFilter
@RegisterRestClient(configKey = "auth-service")
public interface AuthServiceClient {

	@POST
	@Path("/uaa/users")
	@jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
	void createUser(User user);

}
