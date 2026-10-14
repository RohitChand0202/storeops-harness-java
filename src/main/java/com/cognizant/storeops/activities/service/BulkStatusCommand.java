package com.cognizant.storeops.activities.service;

import java.util.List;

import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.shared.domain.StoreOpsUser;

/**
 * One shift handover request, with the authenticated caller rather than a caller-supplied user id.
 *
 * @param actor authenticated staff member
 * @param updates status changes in the order submitted
 */
public record BulkStatusCommand(StoreOpsUser actor, List<ItemUpdate> updates) {

    /** Copies the submitted items so later caller edits cannot change the operation. */
    public BulkStatusCommand {
        updates = List.copyOf(updates);
    }

    /**
     * Returns a snapshot of requested items.
     *
     * @return the requested updates
     */
    public List<ItemUpdate> updates() {
        return List.copyOf(updates);
    }

    /**
     * One requested activity transition.
     *
     * @param taskId activity id
     * @param targetStatus target status
     * @param note optional audit note
     */
    public record ItemUpdate(String taskId, TaskStatus targetStatus, String note) {
    }
}
