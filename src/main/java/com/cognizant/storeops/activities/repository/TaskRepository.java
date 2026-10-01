package com.cognizant.storeops.activities.repository;

import java.util.List;
import java.util.Optional;

import com.cognizant.storeops.activities.domain.Task;
import com.cognizant.storeops.activities.domain.TaskStatus;

/**
 * Data access for activities. Module-private under gate ARCH-1: no class outside the activities
 * module may reference this package. Written as if a database were behind it so the boundary is
 * real; the implementation is a map.
 */
public interface TaskRepository {

    /**
     * Finds one activity.
     *
     * @param id activity id
     * @return the activity, or empty
     */
    Optional<Task> findById(String id);

    /**
     * Finds many activities by id, skipping ids that do not resolve.
     *
     * @param ids activity ids
     * @return the activities that exist, in repository order
     */
    List<Task> findAllById(List<String> ids);

    /**
     * Lists activities, optionally filtered.
     *
     * @param programmeId programme to filter by, or null for all
     * @param status status to filter by, or null for all
     * @return matching activities
     */
    List<Task> findAll(String programmeId, TaskStatus status);

    /**
     * Stores an activity, inserting or replacing by id.
     *
     * @param task the activity
     * @return the stored activity
     */
    Task save(Task task);

    /**
     * Removes an activity.
     *
     * @param id activity id
     * @return true if something was removed
     */
    boolean deleteById(String id);
}
