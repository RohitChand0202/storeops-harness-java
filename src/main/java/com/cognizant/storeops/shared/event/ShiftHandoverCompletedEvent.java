package com.cognizant.storeops.shared.event;

import java.time.Instant;
import java.util.List;

/**
 * At least one activity changed during a shift handover. Consumed by alerts.
 *
 * @param storeId store where the handover occurred
 * @param handoverByUserId authenticated outgoing staff member
 * @param updatedTaskIds activities that actually changed
 * @param occurredAt completion time
 */
public record ShiftHandoverCompletedEvent(
        String storeId,
        String handoverByUserId,
        List<String> updatedTaskIds,
        Instant occurredAt) implements StoreOpsEvent {

    /** Copies the successful ids before dispatching this event. */
    public ShiftHandoverCompletedEvent {
        updatedTaskIds = List.copyOf(updatedTaskIds);
    }

    /**
     * Returns the activities changed by the handover.
     *
     * @return a snapshot of updated activity ids
     */
    public List<String> updatedTaskIds() {
        return List.copyOf(updatedTaskIds);
    }

    @Override
    public String eventType() {
        return "SHIFT_HANDOVER_COMPLETED";
    }
}
