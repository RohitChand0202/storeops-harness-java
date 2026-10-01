package com.cognizant.storeops.shared.event;

import java.time.Instant;

/**
 * A regional rollup was requested. Published and consumed by reports: deferred work, recorded
 * as having been asked for, which is the one same-module event in the catalogue.
 *
 * @param regionId region to roll up
 * @param requestedByUserId who asked
 * @param occurredAt when it happened
 */
public record RegionalRollupRequestedEvent(
        String regionId,
        String requestedByUserId,
        Instant occurredAt) implements StoreOpsEvent {

    @Override
    public String eventType() {
        return "REGIONAL_ROLLUP_REQUESTED";
    }
}
