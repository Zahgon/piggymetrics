package com.piggymetrics.auth.service;

import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class UserServiceTest {

	@Inject
	UserService userService;

	@Inject
	UserRepository repository;

	@BeforeEach
	void cleanUp() {
		repository.deleteAll();
	}

	@Test
	void shouldCreateUser() {

		User user = new User();
		user.setUsername("name");
		user.setPassword("password");

		userService.create(user);

		Optional<User> saved = repository.findByUsername("name");
		assertTrue(saved.isPresent());
		assertEquals("name", saved.get().getUsername());
		assertNotEquals("password", saved.get().getPassword());
		assertTrue(saved.get().getPassword().startsWith("$2a$"), "hash must stay Spring-BCrypt compatible");
		assertTrue(BcryptUtil.matches("password", saved.get().getPassword()));
	}

	@Test
	void shouldFailWhenUserAlreadyExists() {

		User user = new User();
		user.setUsername("name");
		user.setPassword("password");
		userService.create(user);

		User duplicate = new User();
		duplicate.setUsername("name");
		duplicate.setPassword("password");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> userService.create(duplicate));
		assertEquals("user already exists: name", e.getMessage());
	}

	/**
	 * The seeded PiggyMetrics data uses hashes produced by Spring Security's
	 * {@code BCryptPasswordEncoder} ({@code $2a$10$...}); this asserts that the Quarkus
	 * {@code BcryptUtil} accepts them unchanged, so existing users keep working.
	 */
	@Test
	void shouldMatchSpringGeneratedBcryptHash() {
		String springStyleHash = "$2a$10$iMXqlDQ.rIAuOu5riBt9JOtKU6oNKSodtjM3aB8Qjtg.AFIMNGjWu";
		assertTrue(BcryptUtil.matches("password", springStyleHash));
		assertTrue(!BcryptUtil.matches("wrong", springStyleHash));
	}
}
