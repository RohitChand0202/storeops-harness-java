package com.cognizant.storeops.shared.event;

import java.time.Instant;

/**
 * A HIGH or CRITICAL task passed its due date without reaching DONE. Consumed by alerts.
 *
 * @param taskId task that breached
 * @param assigneeId who it is assigned to
 * @param priority task priority
 * @param storeId store it belongs to
 * @param occurredAt when it happened
 */
public record SlaBreachedEvent(
        String taskId,
        String assigneeId,
        String priority,
        String storeId,
        Instant occurredAt) implements StoreOpsEvent {

    @Override
    public String eventType() {
        return "SLA_BREACHED";
    }
}
