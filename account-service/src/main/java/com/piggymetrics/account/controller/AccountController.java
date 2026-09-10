package com.piggymetrics.account.controller;

import com.piggymetrics.account.domain.Account;
import com.piggymetrics.account.domain.User;
import com.piggymetrics.account.service.AccountService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountController {

	/**
	 * Scope granted to trusted back-end services; replaces the Spring
	 * {@code @PreAuthorize("#oauth2.hasScope('server') or #name.equals('demo')")} check.
	 */
	private static final String SERVER_SCOPE = "server";

	private static final String DEMO_ACCOUNT = "demo";

	@Inject
	AccountService accountService;

	@Inject
	SecurityIdentity identity;

	@GET
	@Path("/{name}")
	public Account getAccountByName(@PathParam("name") String name, @Context SecurityContext securityContext) {
		if (!DEMO_ACCOUNT.equals(name)) {
			if (securityContext.getUserPrincipal() == null || identity.isAnonymous()) {
				throw new UnauthorizedException();
			}
			if (!identity.hasRole(SERVER_SCOPE)) {
				throw new ForbiddenException();
			}
		}
		return accountService.findByName(name);
	}

	@GET
	@Path("/current")
	@Authenticated
	public Account getCurrentAccount(@Context SecurityContext securityContext) {
		return accountService.findByName(securityContext.getUserPrincipal().getName());
	}

	@PUT
	@Path("/current")
	@Authenticated
	@ResponseStatus(200)
	public void saveCurrentAccount(@Context SecurityContext securityContext, @Valid Account account) {
		accountService.saveChanges(securityContext.getUserPrincipal().getName(), account);
	}

	@POST
	@Path("/")
	public Account createNewAccount(@Valid User user) {
		return accountService.create(user);
	}
}
