package com.cognizant.storeops.alerts.domain;

import java.time.Instant;

/**
 * An in-app alert. The alerts module is event-driven only: nothing outside it constructs one of
 * these, which is what gate ARCH-2 enforces.
 */
public class Notification {

    private final String id;
    private final String userId;
    private final AlertType alertType;
    private final NotificationChannel channel;
    private final String payload;
    private final Instant createdAt;
    private NotificationStatus status;

    /**
     * Creates a pending notification.
     *
     * @param id stable id
     * @param userId recipient
     * @param alertType what it arose from
     * @param channel how it should reach them
     * @param payload rendered detail
     * @param createdAt when it was raised
     */
    public Notification(String id, String userId, AlertType alertType, NotificationChannel channel,
            String payload, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.alertType = alertType;
        this.channel = channel;
        this.payload = payload;
        this.createdAt = createdAt;
        this.status = NotificationStatus.PENDING;
    }

    /**
     * Notification id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Recipient.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * What the alert arose from.
     *
     * @return the alert type
     */
    public AlertType getAlertType() {
        return alertType;
    }

    /**
     * How it should reach the recipient.
     *
     * @return the channel
     */
    public NotificationChannel getChannel() {
        return channel;
    }

    /**
     * Rendered detail.
     *
     * @return the payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * When it was raised.
     *
     * @return the creation instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Delivery state.
     *
     * @return the status
     */
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * Sets the delivery state.
     *
     * @param status the new status
     */
    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
}
