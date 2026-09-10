package com.piggymetrics.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

// was: AuthServiceApplicationTests#contextLoads
@QuarkusTest
class AuthServiceApplicationTest {

	@Test
	void contextLoads() {
		given()
				.when().get("/q/health/ready")
				.then()
				.statusCode(200);
	}

	@Test
	void shouldPublishSigningKeyAsJwks() {
		given()
				.when().get("/.well-known/jwks.json")
				.then()
				.statusCode(200)
				.body("keys[0].kty", is("RSA"))
				.body("keys[0].alg", is("RS256"))
				.body("keys[0].use", is("sig"))
				.body("keys[0].kid", notNullValue())
				.body("keys[0].n", notNullValue())
				.body("keys[0].e", is("AQAB"));
	}
}
