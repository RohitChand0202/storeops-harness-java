package com.cognizant.storeops.activities.service;

import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;
import com.cognizant.storeops.activities.domain.TaskStatus;

/**
 * A partial update. A null field means "leave unchanged", which is what PATCH means.
 *
 * @param status target status, or null
 * @param priority new priority, or null
 * @param category new category, or null
 * @param assigneeId new assignee, or null
 */
public record UpdateActivityCommand(
        TaskStatus status,
        TaskPriority priority,
        TaskCategory category,
        String assigneeId) {
}
