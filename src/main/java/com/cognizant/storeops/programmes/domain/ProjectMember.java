package com.cognizant.storeops.programmes.domain;

/**
 * A staff member's membership of one programme.
 *
 * @param projectId programme id
 * @param userId staff member id
 * @param role their role in this programme
 */
public record ProjectMember(String projectId, String userId, ProjectRole role) {
}
