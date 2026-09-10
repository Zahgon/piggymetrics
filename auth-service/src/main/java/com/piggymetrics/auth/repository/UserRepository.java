package com.piggymetrics.auth.repository;

import com.piggymetrics.auth.domain.User;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

// was: interface UserRepository extends CrudRepository<User, String>
@ApplicationScoped
public class UserRepository implements PanacheMongoRepositoryBase<User, String> {

	public Optional<User> findByUsername(String username) {
		return Optional.ofNullable(findById(username));
	}

	/**
	 * Inserts or updates the given user, keyed by its username (the Mongo {@code _id}).
	 * Panache equivalent of the former Spring Data {@code CrudRepository#save}.
	 */
	public void save(User user) {
		persistOrUpdate(user);
	}
}
