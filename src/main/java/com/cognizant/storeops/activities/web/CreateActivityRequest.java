package com.cognizant.storeops.activities.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;
import com.cognizant.storeops.activities.service.CreateActivityCommand;

/**
 * Inbound body for raising an activity. Shape validation is declarative and lives here; anything
 * that needs to look at an entity's current state is a business rule and lives in the service.
 *
 * @param title short description of the work
 * @param description optional longer detail
 * @param storeId store the work belongs to
 * @param priority how urgent it is
 * @param category what kind of work it is
 * @param programmeId programme it belongs to, or null
 * @param assigneeId staff member to assign, or null
 */
public record CreateActivityRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotBlank String storeId,
        @NotNull TaskPriority priority,
        @NotNull TaskCategory category,
        String programmeId,
        String assigneeId) {

    /**
     * Converts to the service command, so the service never sees a web type.
     *
     * @return the command
     */
    public CreateActivityCommand toCommand() {
        return new CreateActivityCommand(title, description, storeId, priority, category,
                programmeId, assigneeId);
    }
}
