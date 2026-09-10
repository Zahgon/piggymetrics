package com.piggymetrics.account.controller;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ErrorHandler implements ExceptionMapper<IllegalArgumentException> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	// TODO remove such general handler
	@Override
	public Response toResponse(IllegalArgumentException e) {
		log.info("Returning HTTP 400 Bad Request", e);
		return Response.status(Response.Status.BAD_REQUEST).build();
	}
}
