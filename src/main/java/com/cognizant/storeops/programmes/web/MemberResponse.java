package com.cognizant.storeops.programmes.web;

import com.cognizant.storeops.programmes.domain.ProjectMember;

/**
 * Outbound shape of a programme membership.
 *
 * @param projectId programme id
 * @param userId staff member id
 * @param role their role in this programme
 */
public record MemberResponse(String projectId, String userId, String role) {

    /**
     * Maps an entity to the wire shape.
     *
     * @param member the membership
     * @return its response shape
     */
    public static MemberResponse from(ProjectMember member) {
        return new MemberResponse(member.projectId(), member.userId(), member.role().name());
    }
}
