package com.cognizant.storeops.activities.service;

import java.util.List;

import com.cognizant.storeops.activities.domain.TaskStatus;

/**
 * Per-item results for a shift handover. A reported success means a transition and audit entry exist.
 *
 * @param succeeded applied transitions in request order
 * @param failed rejected items in request order
 */
public record BulkStatusResult(List<UpdatedItem> succeeded, List<FailedItem> failed) {

    /** Copies both outcome lists to preserve the result of this operation. */
    public BulkStatusResult {
        succeeded = List.copyOf(succeeded);
        failed = List.copyOf(failed);
    }

    /**
     * Returns applied transitions.
     *
     * @return a snapshot of applied transitions
     */
    public List<UpdatedItem> succeeded() {
        return List.copyOf(succeeded);
    }

    /**
     * Returns rejected items.
     *
     * @return a snapshot of rejected items
     */
    public List<FailedItem> failed() {
        return List.copyOf(failed);
    }

    /**
     * One applied transition.
     *
     * @param taskId activity id
     * @param previousStatus status before the handover
     * @param newStatus status after the handover
     */
    public record UpdatedItem(String taskId, TaskStatus previousStatus, TaskStatus newStatus) {
    }

    /**
     * One rejected item.
     *
     * @param taskId activity id
     * @param reason stable error code
     */
    public record FailedItem(String taskId, String reason) {
    }
}
