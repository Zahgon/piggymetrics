package com.piggymetrics.account.controller;

import com.piggymetrics.account.domain.Account;
import com.piggymetrics.account.domain.Currency;
import com.piggymetrics.account.domain.Item;
import com.piggymetrics.account.domain.Saving;
import com.piggymetrics.account.domain.TimePeriod;
import com.piggymetrics.account.domain.User;
import com.piggymetrics.account.service.AccountService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class AccountControllerTest {

	@InjectMock
	AccountService accountService;

	@Test
	@TestSecurity(user = "test", roles = { "server" })
	void shouldGetAccountByName() {

		final Account account = new Account();
		account.setName("test");

		when(accountService.findByName(account.getName())).thenReturn(account);

		given()
				.when().get("/" + account.getName())
				.then()
				.statusCode(200)
				.body("name", is(account.getName()));
	}

	@Test
	@TestSecurity(user = "test")
	void shouldGetCurrentAccount() {

		final Account account = new Account();
		account.setName("test");

		when(accountService.findByName(account.getName())).thenReturn(account);

		given()
				.when().get("/current")
				.then()
				.statusCode(200)
				.body("name", is(account.getName()));
	}

	@Test
	@TestSecurity(user = "test")
	void shouldSaveCurrentAccount() {

		Saving saving = new Saving();
		saving.setAmount(new BigDecimal(1500));
		saving.setCurrency(Currency.USD);
		saving.setInterest(new BigDecimal("3.32"));
		saving.setDeposit(true);
		saving.setCapitalization(false);

		Item grocery = new Item();
		grocery.setTitle("Grocery");
		grocery.setAmount(new BigDecimal(10));
		grocery.setCurrency(Currency.USD);
		grocery.setPeriod(TimePeriod.DAY);
		grocery.setIcon("meal");

		Item salary = new Item();
		salary.setTitle("Salary");
		salary.setAmount(new BigDecimal(9100));
		salary.setCurrency(Currency.USD);
		salary.setPeriod(TimePeriod.MONTH);
		salary.setIcon("wallet");

		final Account account = new Account();
		account.setName("test");
		account.setNote("test note");
		account.setLastSeen(new Date());
		account.setSaving(saving);
		account.setExpenses(List.of(grocery));
		account.setIncomes(List.of(salary));

		given()
				.contentType(ContentType.JSON)
				.body(account)
				.when().put("/current")
				.then()
				.statusCode(200);
	}

	@Test
	@TestSecurity(user = "test")
	void shouldFailOnValidationTryingToSaveCurrentAccount() {

		final Account account = new Account();
		account.setName("test");

		given()
				.contentType(ContentType.JSON)
				.body(account)
				.when().put("/current")
				.then()
				.statusCode(400);
	}

	@Test
	@TestSecurity(user = "test")
	void shouldRegisterNewAccount() {

		final User user = new User();
		user.setUsername("test");
		user.setPassword("password");

		final Account created = new Account();
		created.setName(user.getUsername());

		when(accountService.create(any(User.class))).thenReturn(created);

		given()
				.contentType(ContentType.JSON)
				.body(user)
				.when().post("/")
				.then()
				.statusCode(200);
	}

	@Test
	@TestSecurity(user = "test")
	void shouldFailOnValidationTryingToRegisterNewAccount() {

		final User user = new User();
		user.setUsername("t");

		given()
				.contentType(ContentType.JSON)
				.body(user)
				.when().post("/")
				.then()
				.statusCode(400);
	}
}
