package com.piggymetrics.auth.controller;

import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.service.UserService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

@QuarkusTest
class UserControllerTest {

	@InjectMock
	UserService userService;

	@Test
	@TestSecurity(user = "test", roles = { "server" })
	void shouldCreateNewUser() {

		final User user = new User();
		user.setUsername("test");
		user.setPassword("password");

		given()
				.contentType(ContentType.JSON)
				.body(user)
				.when().post("/users")
				.then()
				.statusCode(200);

		verify(userService).create(org.mockito.ArgumentMatchers.any(User.class));
	}

	@Test
	@TestSecurity(user = "test", roles = { "server" })
	void shouldFailWhenUserIsNotValid() {
		// was: mockMvc.perform(post("/users")) - no body at all
		given()
				.contentType(ContentType.JSON)
				.when().post("/users")
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "test")
	void shouldReturnCurrentUser() {
		given()
				.when().get("/users/current")
				.then()
				.statusCode(200)
				.body("name", is("test"));
	}

	@Test
	void shouldRejectUserCreationWithoutServerScope() {
		given()
				.contentType(ContentType.JSON)
				.body(new User())
				.when().post("/users")
				.then()
				.statusCode(401);
	}
}
