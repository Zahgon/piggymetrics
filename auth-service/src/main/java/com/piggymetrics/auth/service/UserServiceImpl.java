package com.piggymetrics.auth.service;

import com.piggymetrics.auth.domain.User;
import com.piggymetrics.auth.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class UserServiceImpl implements UserService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserRepository repository;

	@Inject
	public UserServiceImpl(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	public void create(User user) {

		Optional<User> existing = repository.findByUsername(user.getUsername());
		existing.ifPresent(it -> {
			throw new IllegalArgumentException("user already exists: " + it.getUsername());
		});

		// was: new BCryptPasswordEncoder().encode(...) - BcryptUtil emits the same $2a$ format
		String hash = BcryptUtil.bcryptHash(user.getPassword());
		user.setPassword(hash);

		repository.save(user);

		log.info("new user has been created: {}", user.getUsername());
	}
}
