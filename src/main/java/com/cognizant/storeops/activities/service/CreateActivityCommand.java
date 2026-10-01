package com.cognizant.storeops.activities.service;

import com.cognizant.storeops.activities.domain.TaskCategory;
import com.cognizant.storeops.activities.domain.TaskPriority;

/**
 * What the service needs to raise an activity. A command, not a web DTO: the service never accepts
 * or returns a type from the web layer.
 *
 * @param title short description of the work
 * @param description optional longer detail
 * @param storeId store the work belongs to
 * @param priority how urgent it is
 * @param category what kind of work it is
 * @param programmeId programme it belongs to, or null
 * @param assigneeId staff member to assign, or null
 */
public record CreateActivityCommand(
        String title,
        String description,
        String storeId,
        TaskPriority priority,
        TaskCategory category,
        String programmeId,
        String assigneeId) {
}
