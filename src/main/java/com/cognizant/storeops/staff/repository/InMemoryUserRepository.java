package com.cognizant.storeops.staff.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.cognizant.storeops.staff.domain.AuthToken;
import com.cognizant.storeops.staff.domain.User;

/** In-memory staff store. */
@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, AuthToken> tokens = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<AuthToken> findToken(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    @Override
    public AuthToken saveToken(AuthToken token) {
        tokens.put(token.token(), token);
        return token;
    }
}
