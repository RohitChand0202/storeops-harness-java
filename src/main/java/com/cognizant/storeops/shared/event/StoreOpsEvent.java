package com.cognizant.storeops.shared.event;

import java.time.Instant;

/** Anything raised on the bus. Events are records, named in the past tense. */
public interface StoreOpsEvent {

    /**
     * Stable event type name.
     *
     * @return the type
     */
    String eventType();

    /**
     * When the thing that this event describes happened.
     *
     * @return the instant
     */
    Instant occurredAt();
}
