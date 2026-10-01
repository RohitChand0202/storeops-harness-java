package com.cognizant.storeops.staff.domain;

/**
 * A store staff member. The staff module is read-only to every other module: no other module may
 * create, update or delete one.
 *
 * @param id user id
 * @param email login email
 * @param name display name
 * @param role their role
 * @param storeId store they work in
 * @param regionId region that store belongs to
 * @param active whether the account is enabled
 */
public record User(
        String id,
        String email,
        String name,
        StaffRole role,
        String storeId,
        String regionId,
        boolean active) {
}
