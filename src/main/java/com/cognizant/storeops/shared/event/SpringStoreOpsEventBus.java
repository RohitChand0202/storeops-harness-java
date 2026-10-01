package com.cognizant.storeops.shared.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The one class permitted to touch ApplicationEventPublisher. It lives in shared.event because
 * ARCH-2's third rule forbids every class outside this package from depending on that type.
 */
@Component
public class SpringStoreOpsEventBus implements StoreOpsEventBus {

    private final ApplicationEventPublisher publisher;

    /**
     * Wraps Spring's publisher.
     *
     * @param publisher the Spring publisher
     */
    public SpringStoreOpsEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(StoreOpsEvent event) {
        publisher.publishEvent(event);
    }
}
