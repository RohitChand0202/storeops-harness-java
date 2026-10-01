package com.cognizant.storeops.activities.service;

import java.util.List;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;

/**
 * Business logic for operational activities. This interface is what other modules see; gate ARCH-1
 * depends on there being something to depend on that is not an implementation.
 */
public interface ActivityService {

    /**
     * Lists activities, optionally filtered.
     *
     * @param programmeId programme to filter by, or null
     * @param status status to filter by, or null
     * @return matching activities
     */
    List<Task> list(String programmeId, TaskStatus status);

    /**
     * Finds one activity.
     *
     * @param id activity id
     * @return the activity
     */
    Task findById(String id);

    /**
     * Raises a new activity.
     *
     * @param command what to create
     * @return the created activity
     */
    Task create(CreateActivityCommand command);

    /**
     * Applies a partial update, including a status transition.
     *
     * @param id activity id
     * @param command the fields to change
     * @return the updated activity
     */
    Task update(String id, UpdateActivityCommand command);

    /**
     * Removes an activity.
     *
     * @param id activity id
     */
    void delete(String id);
}
