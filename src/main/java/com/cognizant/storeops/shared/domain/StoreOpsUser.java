package com.cognizant.storeops.shared.domain;

/**
 * The authenticated principal, resolved from an AuthToken and injected with
 * {@code @AuthenticationPrincipal}. A read-only projection of the staff module's User, living in
 * shared so that every controller and service in the system does not import a staff type to learn
 * who is calling.
 *
 * @param id user id
 * @param storeId store the caller acts in
 * @param regionId region the store belongs to
 * @param role the caller's role name, from StaffRole
 */
public record StoreOpsUser(String id, String storeId, String regionId, String role) {
}
