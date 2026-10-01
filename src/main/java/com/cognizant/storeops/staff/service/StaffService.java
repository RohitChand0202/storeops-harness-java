package com.cognizant.storeops.staff.service;

import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.staff.domain.User;

/**
 * Staff lookup and authentication. Other modules call this interface for a read; they never reach
 * the staff repository, and they never write a User.
 */
public interface StaffService {

    /**
     * Finds one staff member.
     *
     * @param id user id
     * @return the user
     */
    User findById(String id);

    /**
     * Resolves a bearer token to the authenticated principal.
     *
     * @param token the opaque token value
     * @return the principal
     */
    StoreOpsUser resolveToken(String token);
}
