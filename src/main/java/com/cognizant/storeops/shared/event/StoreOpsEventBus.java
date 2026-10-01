package com.cognizant.storeops.shared.event;

/**
 * The only legal route for a cross-module side effect, per gate ARCH-2. A module publishes; it
 * never injects another module's service to cause a change.
 *
 * <p>Inject this interface, never Spring's ApplicationEventPublisher. ArchUnit's ARCH-2 rule
 * watches that type, and injecting it directly is a legal Spring idiom that routes around the
 * thing the gate inspects.
 */
public interface StoreOpsEventBus {

    /**
     * Raises an event. Delivery is synchronous and in-process.
     *
     * @param event the event to publish
     */
    void publish(StoreOpsEvent event);
}
