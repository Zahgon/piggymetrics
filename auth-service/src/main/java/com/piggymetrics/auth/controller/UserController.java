package com.piggymetrics.auth.controller;

import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.Map;

// was: @RestController @RequestMapping("/users")
@Path("/users")
public class UserController {

	private final UserService userService;

	@Inject
	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * was: {@code public Principal getUser(Principal principal)} which serialized the whole
	 * {@code OAuth2Authentication}. Simplified on purpose to the only field the UI reads
	 * ({@code $.name}); the OAuth2-specific envelope (authorities, details, oauth2Request...)
	 * is intentionally not reproduced.
	 */
	@GET
	@Path("/current")
	@Authenticated
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> getUser(@Context SecurityContext securityContext) {
		return Map.of("name", securityContext.getUserPrincipal().getName());
	}

	// was: @PreAuthorize("#oauth2.hasScope('server')") - the `scope` claim is mapped onto roles
	@POST
	@RolesAllowed("server")
	@Consumes(MediaType.APPLICATION_JSON)
	@ResponseStatus(200)
	public void createUser(@Valid User user) {
		if (user == null) {
			// an empty request body must be a 400, as in shouldFailWhenUserIsNotValid
			throw new BadRequestException("request body is required");
		}
		userService.create(user);
	}
}
