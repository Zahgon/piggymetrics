package com.piggymetrics.config;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * was: @EnableConfigServer (org.springframework.cloud.config.server.EnvironmentController)
 *
 * Reproduces the Config Server `native` profile REST contract over the classpath
 * `shared/` directory. The {label} segment is accepted and echoed back but otherwise
 * ignored, exactly as the native (non-git) backend does.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigServerResource {

    @Inject
    SharedConfigRepository repository;

    @GET
    @Path("/{application}/{profile}")
    public Environment environment(@PathParam("application") String application,
            @PathParam("profile") String profile) {
        return build(application, profile, null);
    }

    @GET
    @Path("/{application}/{profile}/{label}")
    public Environment environment(@PathParam("application") String application,
            @PathParam("profile") String profile,
            @PathParam("label") String label) {
        return build(application, profile, label);
    }

    private Environment build(String application, String profile, String label) {
        List<PropertySource> sources = repository.propertySourcesFor(application);
        return new Environment(application, List.of(profile), label, sources);
    }
}
