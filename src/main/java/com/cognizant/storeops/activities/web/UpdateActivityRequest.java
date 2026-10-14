package com.cognizant.storeops.activities.web;

import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.service.UpdateActivityCommand;

/**
 * Inbound body for a partial update. Every field is optional, which is what PATCH means; a null
 * field leaves the activity unchanged.
 *
 * @param status target status, or null
 * @param priority new priority, or null
 * @param category new category, or null
 * @param assigneeId new assignee, or null
 * @param actingUserId legacy caller field; ignored because identity comes from the bearer token
 */
public record UpdateActivityRequest(
        TaskStatus status,
        TaskPriority priority,
        TaskCategory category,
        String assigneeId,
        String actingUserId) {

    /**
     * Converts to the service command.
     *
     * @return the command
     */
    public UpdateActivityCommand toCommand() {
        return new UpdateActivityCommand(status, priority, category, assigneeId);
    }
}
