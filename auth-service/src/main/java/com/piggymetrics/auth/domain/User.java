package com.piggymetrics.auth.domain;

import io.quarkus.mongodb.panache.common.MongoEntity;
import jakarta.validation.constraints.NotBlank;
import org.bson.codecs.pojo.annotations.BsonId;

/**
 * Plain POJO persisted into the {@code users} collection, keyed by {@code username}
 * (the Mongo {@code _id}).
 *
 * <p>was: {@code @Document(collection = "users")} + Spring Security {@code UserDetails}.
 * The {@code UserDetails} contract (authorities / isEnabled / isAccountNonLocked ...) is
 * dropped: nothing in the original code consumed it beyond the password comparison, and the
 * Quarkus token endpoint reads the BCrypt hash directly.</p>
 */
@MongoEntity(collection = "users")
public class User {

	// was: @org.springframework.data.annotation.Id
	@BsonId
	@NotBlank
	private String username;

	@NotBlank
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
