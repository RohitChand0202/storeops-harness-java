package com.cognizant.storeops.staff.service;

import java.util.List;

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

    /** Finds active department leads for handover delivery in one store. */
    List<User> findDepartmentLeadsForStore(String storeId);

    /**
     * Resolves a bearer token to the authenticated principal.
     *
     * @param token the opaque token value
     * @return the principal
     */
    StoreOpsUser resolveToken(String token);
}
