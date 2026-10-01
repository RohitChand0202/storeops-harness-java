package com.cognizant.storeops.shared.event;

import java.time.Instant;

/**
 * A task moved to BLOCKED. Consumed by alerts.
 *
 * @param taskId task that was blocked
 * @param blockedByUserId who blocked it
 * @param reason why
 * @param occurredAt when it happened
 */
public record TaskBlockedEvent(
        String taskId,
        String blockedByUserId,
        String reason,
        Instant occurredAt) implements StoreOpsEvent {

    @Override
    public String eventType() {
        return "TASK_BLOCKED";
    }
}
