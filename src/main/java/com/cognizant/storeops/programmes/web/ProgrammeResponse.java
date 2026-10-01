package com.cognizant.storeops.programmes.web;

import java.time.Instant;

import com.cognizant.storeops.programmes.domain.Project;

/**
 * Outbound shape of a programme.
 *
 * @param id programme id
 * @param name programme name
 * @param description optional detail
 * @param storeId store it runs in
 * @param ownerId staff member who owns it
 * @param status whether it is still open
 * @param createdAt when it was opened
 */
public record ProgrammeResponse(
        String id,
        String name,
        String description,
        String storeId,
        String ownerId,
        String status,
        Instant createdAt) {

    /**
     * Maps an entity to the wire shape.
     *
     * @param project the programme
     * @return its response shape
     */
    public static ProgrammeResponse from(Project project) {
        return new ProgrammeResponse(project.getId(), project.getName(), project.getDescription(),
                project.getStoreId(), project.getOwnerId(), project.getStatus().name(),
                project.getCreatedAt());
    }
}
