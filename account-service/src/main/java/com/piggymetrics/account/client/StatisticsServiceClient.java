package com.piggymetrics.account.client;

import com.piggymetrics.account.domain.Account;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * {@code @OidcClientFilter} attaches a {@code Bearer} token obtained from the default
 * {@code quarkus.oidc-client} (account-service's own client_credentials, scope {@code server}).
 * This is the replacement for Spring Cloud Security's {@code OAuth2FeignRequestInterceptor}.
 */
@OidcClientFilter
@RegisterRestClient(configKey = "statistics-service")
public interface StatisticsServiceClient {

	@PUT
	@Path("/statistics/{accountName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Fallback(StatisticsServiceClientFallback.class)
	void updateStatistics(@PathParam("accountName") String accountName, Account account);

}
