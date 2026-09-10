package com.piggymetrics.auth.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * Spring MVC treated a bodyless {@code POST /users} as a 400 (missing required body), while
 * JAX-RS answers 415 when the request carries no {@code Content-Type} at all. Assuming JSON
 * for {@code /users} writes keeps the original status contract, and a genuinely unsupported
 * media type still yields 415 because the header is only defaulted when it is absent.
 */
@Provider
@PreMatching
public class DefaultJsonContentTypeFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) {
		String path = requestContext.getUriInfo().getPath();
		boolean writeToUsers = "POST".equals(requestContext.getMethod())
				&& (path.startsWith("users") || path.startsWith("/users"));
		if (writeToUsers && requestContext.getHeaderString(HttpHeaders.CONTENT_TYPE) == null) {
			requestContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		}
	}
}
