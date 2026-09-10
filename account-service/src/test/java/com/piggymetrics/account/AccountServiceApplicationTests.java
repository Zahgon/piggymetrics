package com.piggymetrics.account;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Replacement of the former {@code @SpringBootTest} smoke test: boots the whole Quarkus
 * application (including Dev Services MongoDB) and fails if any bean cannot be wired.
 */
@QuarkusTest
class AccountServiceApplicationTests {

	@Test
	void contextLoads() {

	}

}
