package com.cognizant.storeops.activities.service;

import java.util.List;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.shared.domain.StoreOpsUser;

/**
 * Business logic for operational activities. This interface is what other modules see; gate ARCH-1
 * depends on there being something to depend on that is not an implementation.
 */
public interface ActivityService {

    /**
     * Lists activities, optionally filtered.
     *
     * @param actor authenticated staff member whose store is visible
     * @param programmeId programme to filter by, or null
     * @param status status to filter by, or null
     * @return matching activities
     */
    List<Task> list(StoreOpsUser actor, String programmeId, TaskStatus status);

    /**
     * Finds one activity.
     *
     * @param actor authenticated staff member
     * @param id activity id
     * @return the activity
     */
    Task findById(StoreOpsUser actor, String id);

    /**
     * Raises a new activity.
     *
     * @param actor authenticated staff member
     * @param command what to create
     * @return the created activity
     */
    Task create(StoreOpsUser actor, CreateActivityCommand command);

    /**
     * Applies a partial update, including a status transition.
     *
     * @param actor authenticated staff member and audit actor
     * @param id activity id
     * @param command the fields to change
     * @return the updated activity
     */
    Task update(StoreOpsUser actor, String id, UpdateActivityCommand command);

    /**
     * Applies independent handover transitions and reports each item's outcome.
     *
     * @param command authenticated caller and requested transitions
     * @return applied and rejected items in request order
     */
    BulkStatusResult bulkUpdateStatus(BulkStatusCommand command);

    /**
     * Removes an activity.
     *
     * @param actor authenticated staff member
     * @param id activity id
     */
    void delete(StoreOpsUser actor, String id);
}
