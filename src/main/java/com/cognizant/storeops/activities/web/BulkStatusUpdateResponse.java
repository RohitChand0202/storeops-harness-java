package com.cognizant.storeops.activities.web;

import java.util.List;

import com.cognizant.storeops.activities.service.BulkStatusResult;

/**
 * Outcome of a bulk handover, including counts for the handheld summary view.
 *
 * @param requestedCount number of submitted items
 * @param succeededCount number of applied transitions
 * @param failedCount number of rejected items
 * @param succeeded applied transitions
 * @param failed rejected items
 */
public record BulkStatusUpdateResponse(
        int requestedCount,
        int succeededCount,
        int failedCount,
        List<UpdatedItem> succeeded,
        List<FailedItem> failed) {

    /** Copies the outcome lists before returning them to an HTTP client. */
    public BulkStatusUpdateResponse {
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
     * Maps a service result onto the wire shape.
     *
     * @param result service outcome
     * @return response with counts and both item lists
     */
    public static BulkStatusUpdateResponse from(BulkStatusResult result) {
        List<UpdatedItem> succeeded = result.succeeded().stream()
                .map(item -> new UpdatedItem(item.taskId(), item.previousStatus().name(),
                        item.newStatus().name()))
                .toList();
        List<FailedItem> failed = result.failed().stream()
                .map(item -> new FailedItem(item.taskId(), item.reason()))
                .toList();
        return new BulkStatusUpdateResponse(succeeded.size() + failed.size(),
                succeeded.size(), failed.size(), succeeded, failed);
    }

    /**
     * One applied transition.
     *
     * @param taskId activity id
     * @param previousStatus former status
     * @param newStatus current status
     */
    public record UpdatedItem(String taskId, String previousStatus, String newStatus) {
    }

    /**
     * One rejected item.
     *
     * @param taskId activity id
     * @param reason stable code
     */
    public record FailedItem(String taskId, String reason) {
    }
}
