package com.piggymetrics.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TokenResourceTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** produced by Spring Security's BCryptPasswordEncoder-compatible $2a$ format, for "password" */
	private static final String SPRING_STYLE_HASH = "$2a$10$iMXqlDQ.rIAuOu5riBt9JOtKU6oNKSodtjM3aB8Qjtg.AFIMNGjWu";

	@Inject
	UserRepository repository;

	@BeforeEach
	void seed() {
		repository.deleteAll();

		User demo = new User();
		demo.setUsername("demo");
		demo.setPassword(BcryptUtil.bcryptHash("demo-password"));
		repository.save(demo);

		User legacy = new User();
		legacy.setUsername("legacy");
		legacy.setPassword(SPRING_STYLE_HASH);
		repository.save(legacy);
	}

	@Test
	void shouldIssueUiScopedTokenForPasswordGrant() throws Exception {

		Response response = given()
				.header("Authorization", basic("browser", ""))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "password")
				.formParam("username", "demo")
				.formParam("password", "demo-password")
				.when().post("/oauth/token")
				.then()
				.statusCode(200)
				.extract().response();

		assertEquals("Bearer", response.jsonPath().getString("token_type"));
		assertEquals(43200, response.jsonPath().getInt("expires_in"));
		assertEquals("ui", response.jsonPath().getString("scope"));

		JsonNode claims = decodeClaims(response.jsonPath().getString("access_token"));
		assertEquals("ui", claims.get("scope").asText());
		assertEquals("demo", claims.get("sub").asText());
		assertEquals("browser", claims.get("client_id").asText());
	}

	@Test
	void shouldRejectBrowserRequestingServerScope() {
		given()
				.header("Authorization", basic("browser", ""))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "password")
				.formParam("username", "demo")
				.formParam("password", "demo-password")
				.formParam("scope", "server")
				.when().post("/oauth/token")
				.then()
				.statusCode(400)
				.body("error", org.hamcrest.Matchers.is("invalid_scope"));
	}

	@Test
	void shouldIssueServerScopedTokenForClientCredentialsGrant() throws Exception {

		Response response = given()
				.header("Authorization", basic("account-service", "test-account-secret"))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "client_credentials")
				.when().post("/oauth/token")
				.then()
				.statusCode(200)
				.extract().response();

		assertEquals("server", response.jsonPath().getString("scope"));

		JsonNode claims = decodeClaims(response.jsonPath().getString("access_token"));
		assertEquals("server", claims.get("scope").asText());
		assertEquals("account-service", claims.get("client_id").asText());
	}

	@Test
	void shouldRejectClientCredentialsWithWrongSecret() {
		given()
				.header("Authorization", basic("account-service", "nope"))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "client_credentials")
				.when().post("/oauth/token")
				.then()
				.statusCode(401);
	}

	@Test
	void shouldRejectBadUserPassword() {
		given()
				.header("Authorization", basic("browser", ""))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "password")
				.formParam("username", "demo")
				.formParam("password", "wrong")
				.when().post("/oauth/token")
				.then()
				.statusCode(400)
				.body("error", org.hamcrest.Matchers.is("invalid_grant"));
	}

	@Test
	void shouldAuthenticateUserStoredWithSpringGeneratedHash() {
		given()
				.header("Authorization", basic("browser", ""))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "password")
				.formParam("username", "legacy")
				.formParam("password", "password")
				.when().post("/oauth/token")
				.then()
				.statusCode(200)
				.body("scope", org.hamcrest.Matchers.is("ui"));
	}

	@Test
	void shouldRejectRefreshTokenGrant() {
		given()
				.header("Authorization", basic("browser", ""))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "refresh_token")
				.formParam("refresh_token", "whatever")
				.when().post("/oauth/token")
				.then()
				.statusCode(400)
				.body("error", org.hamcrest.Matchers.is("unsupported_grant_type"));
	}

	@Test
	void issuedTokenIsAcceptedByThisServiceAsBearer() {

		String token = given()
				.header("Authorization", basic("account-service", "test-account-secret"))
				.contentType(ContentType.URLENC)
				.formParam("grant_type", "client_credentials")
				.when().post("/oauth/token")
				.then().statusCode(200)
				.extract().jsonPath().getString("access_token");

		// the token really is signed and verifiable: it opens the server-scoped endpoint
		String created = "created-" + System.nanoTime();
		given()
				.header("Authorization", "Bearer " + token)
				.contentType(ContentType.JSON)
				.body("{\"username\":\"" + created + "\",\"password\":\"secret\"}")
				.when().post("/users")
				.then()
				.statusCode(200);

		assertTrue(repository.findByUsername(created).isPresent());
		assertNotEquals("secret", repository.findByUsername(created).get().getPassword());
	}

	private static JsonNode decodeClaims(String jwt) throws Exception {
		String[] parts = jwt.split("\\.");
		assertEquals(3, parts.length, "an RS256 JWT must have three parts");
		return MAPPER.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
	}

	private static String basic(String clientId, String secret) {
		return "Basic " + Base64.getEncoder()
				.encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
	}
}
