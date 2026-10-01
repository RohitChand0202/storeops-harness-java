package com.cognizant.storeops.staff.domain;

/**
 * Profile detail for a staff member.
 *
 * @param userId user id
 * @param displayName name shown on a handheld
 * @param department department they work in
 * @param phone contact number
 */
public record UserProfile(String userId, String displayName, String department, String phone) {
}
