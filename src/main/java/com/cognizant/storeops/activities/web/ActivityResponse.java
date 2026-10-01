package com.cognizant.storeops.activities.web;

import java.time.Instant;

import com.cognizant.storeops.activities.domain.Task;

/**
 * Outbound shape of an activity. Entities are not DTOs: mapping between them happens here and
 * nowhere else.
 *
 * @param id activity id
 * @param title short description of the work
 * @param description optional longer detail
 * @param status current status
 * @param priority current priority
 * @param category what kind of work it is
 * @param programmeId programme it belongs to, or null
 * @param assigneeId staff member it is assigned to, or null
 * @param storeId store it belongs to
 * @param dueDate when it is due, or null
 * @param createdAt when it was raised
 * @param updatedAt when it last changed
 */
public record ActivityResponse(
        String id,
        String title,
        String description,
        String status,
        String priority,
        String category,
        String programmeId,
        String assigneeId,
        String storeId,
        Instant dueDate,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps an entity to the wire shape.
     *
     * @param task the activity
     * @return its response shape
     */
    public static ActivityResponse from(Task task) {
        return new ActivityResponse(task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus().name(), task.getPriority().name(), task.getCategory().name(),
                task.getProgrammeId(), task.getAssigneeId(), task.getStoreId(),
                task.getDueDate(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
