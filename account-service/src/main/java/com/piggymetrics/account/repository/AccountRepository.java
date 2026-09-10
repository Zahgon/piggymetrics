package com.piggymetrics.account.repository;

import com.piggymetrics.account.domain.Account;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountRepository implements PanacheMongoRepositoryBase<Account, String> {

	public Account findByName(String name) {
		return find("_id", name).firstResult();
	}

	/**
	 * Inserts or updates the given account, keyed by its name (the Mongo {@code _id}).
	 * Panache equivalent of the former Spring Data {@code CrudRepository#save}.
	 */
	public void save(Account account) {
		persistOrUpdate(account);
	}
}
