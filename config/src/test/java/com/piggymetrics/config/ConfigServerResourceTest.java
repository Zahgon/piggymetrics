package com.piggymetrics.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Contract tests for the Config Server `native` profile REST shape.
 */
@QuarkusTest
class ConfigServerResourceTest {

    private static final String USER = "user";
    private static final String PASSWORD = "test-config-secret";

    @Test
    void shouldRejectAnonymousAccess() {
        given()
                .when().get("/account-service/native")
                .then().statusCode(401);
    }

    @Test
    void shouldServeApplicationSpecificPropertiesFirst() {
        given()
                .auth().preemptive().basic(USER, PASSWORD)
                .when().get("/account-service/native")
                .then()
                .statusCode(200)
                .body("name", equalTo("account-service"))
                .body("profiles", hasSize(1))
                .body("profiles[0]", equalTo("native"))
                .body("label", is(nullValue()))
                .body("version", is(nullValue()))
                .body("state", is(nullValue()))
                .body("propertySources", hasSize(2))
                .body("propertySources[0].name", endsWith("account-service.yml"))
                .body("propertySources[0].source.'server.port'", equalTo(6000))
                .body("propertySources[0].source.'feign.hystrix.enabled'", equalTo(true))
                // ${VAR} placeholders are passed through literally
                .body("propertySources[0].source.'spring.data.mongodb.password'", equalTo("${MONGODB_PASSWORD}"))
                .body("propertySources[1].name", endsWith("application.yml"));
    }

    @Test
    void shouldEchoLabelWhenProvided() {
        given()
                .auth().preemptive().basic(USER, PASSWORD)
                .when().get("/account-service/native/master")
                .then()
                .statusCode(200)
                .body("label", equalTo("master"));
    }

    @Test
    void shouldSkipEmptyDocuments() {
        given()
                .auth().preemptive().basic(USER, PASSWORD)
                .when().get("/monitoring/native")
                .then()
                .statusCode(200)
                .body("name", equalTo("monitoring"))
                .body("propertySources", hasSize(1))
                .body("propertySources[0].name", endsWith("application.yml"));
    }

    @Test
    void shouldReturnSharedDefaultsForUnknownApplication() {
        given()
                .auth().preemptive().basic(USER, PASSWORD)
                .when().get("/does-not-exist/native")
                .then()
                .statusCode(200)
                .body("propertySources", hasSize(1))
                .body("propertySources[0].name", endsWith("application.yml"));
    }
}
