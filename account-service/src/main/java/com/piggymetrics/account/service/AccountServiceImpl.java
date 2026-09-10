package com.piggymetrics.account.service;

import com.piggymetrics.account.client.AuthServiceClient;
import com.piggymetrics.account.client.StatisticsServiceClient;
import com.piggymetrics.account.domain.Account;
import com.piggymetrics.account.domain.Currency;
import com.piggymetrics.account.domain.Saving;
import com.piggymetrics.account.domain.User;
import com.piggymetrics.account.repository.AccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Date;

@ApplicationScoped
public class AccountServiceImpl implements AccountService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final StatisticsServiceClient statisticsClient;

	private final AuthServiceClient authClient;

	private final AccountRepository repository;

	@Inject
	public AccountServiceImpl(@RestClient StatisticsServiceClient statisticsClient,
			@RestClient AuthServiceClient authClient,
			AccountRepository repository) {
		this.statisticsClient = statisticsClient;
		this.authClient = authClient;
		this.repository = repository;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Account findByName(String accountName) {
		if (accountName == null || accountName.isEmpty()) {
			throw new IllegalArgumentException("[Assertion failed] - this String argument must have length; it must not be null or empty");
		}
		return repository.findByName(accountName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Account create(User user) {

		Account existing = repository.findByName(user.getUsername());
		if (existing != null) {
			throw new IllegalArgumentException("account already exists: " + user.getUsername());
		}

		authClient.createUser(user);

		Saving saving = new Saving();
		saving.setAmount(new BigDecimal(0));
		saving.setCurrency(Currency.getDefault());
		saving.setInterest(new BigDecimal(0));
		saving.setDeposit(false);
		saving.setCapitalization(false);

		Account account = new Account();
		account.setName(user.getUsername());
		account.setLastSeen(new Date());
		account.setSaving(saving);

		repository.save(account);

		log.info("new account has been created: " + account.getName());

		return account;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveChanges(String name, Account update) {

		Account account = repository.findByName(name);
		if (account == null) {
			throw new IllegalArgumentException("can't find account with name " + name);
		}

		account.setIncomes(update.getIncomes());
		account.setExpenses(update.getExpenses());
		account.setSaving(update.getSaving());
		account.setNote(update.getNote());
		account.setLastSeen(new Date());
		repository.save(account);

		log.debug("account {} changes has been saved", name);

		statisticsClient.updateStatistics(name, account);
	}
}
