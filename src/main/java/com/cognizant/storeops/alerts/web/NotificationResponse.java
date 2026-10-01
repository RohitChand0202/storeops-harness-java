package com.cognizant.storeops.alerts.web;

import java.time.Instant;

import com.cognizant.storeops.alerts.domain.Notification;

/**
 * Outbound shape of an alert.
 *
 * @param id notification id
 * @param userId recipient
 * @param alertType what it arose from
 * @param channel how it should reach them
 * @param status delivery state
 * @param payload rendered detail
 * @param createdAt when it was raised
 */
public record NotificationResponse(
        String id,
        String userId,
        String alertType,
        String channel,
        String status,
        String payload,
        Instant createdAt) {

    /**
     * Maps an entity to the wire shape.
     *
     * @param notification the alert
     * @return its response shape
     */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getUserId(),
                notification.getAlertType().name(), notification.getChannel().name(),
                notification.getStatus().name(), notification.getPayload(),
                notification.getCreatedAt());
    }
}
