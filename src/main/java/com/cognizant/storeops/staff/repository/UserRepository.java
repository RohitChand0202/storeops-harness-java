package com.cognizant.storeops.staff.repository;

import java.util.List;
import java.util.Optional;

import com.cognizant.storeops.staff.domain.AuthToken;
import com.cognizant.storeops.staff.domain.User;

/** Data access for staff. Module-private under gate ARCH-1. */
public interface UserRepository {

    /**
     * Finds one staff member.
     *
     * @param id user id
     * @return the user, or empty
     */
    Optional<User> findById(String id);

    /** Lists staff assigned to one store. */
    List<User> findByStoreId(String storeId);

    /**
     * Stores a staff member.
     *
     * @param user the user
     * @return the stored user
     */
    User save(User user);

    /**
     * Finds a token.
     *
     * @param token the token value
     * @return the token, or empty
     */
    Optional<AuthToken> findToken(String token);

    /**
     * Stores a token.
     *
     * @param token the token
     * @return the stored token
     */
    AuthToken saveToken(AuthToken token);
}
