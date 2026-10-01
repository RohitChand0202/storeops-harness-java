package com.cognizant.storeops.shared.event;

import java.time.Instant;

/**
 * A programme closed. Consumed by reports and alerts.
 *
 * @param programmeId programme that closed
 * @param storeId its store
 * @param closedByUserId who closed it
 * @param occurredAt when it happened
 */
public record ProgrammeClosedEvent(
        String programmeId,
        String storeId,
        String closedByUserId,
        Instant occurredAt) implements StoreOpsEvent {

    @Override
    public String eventType() {
        return "PROGRAMME_CLOSED";
    }
}
