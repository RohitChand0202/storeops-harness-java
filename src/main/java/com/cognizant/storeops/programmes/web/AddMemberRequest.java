package com.cognizant.storeops.programmes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.cognizant.storeops.programmes.domain.ProjectRole;

/**
 * Inbound body for adding a staff member to a programme.
 *
 * @param userId staff member id
 * @param role their role in this programme
 */
public record AddMemberRequest(@NotBlank String userId, @NotNull ProjectRole role) {
}
