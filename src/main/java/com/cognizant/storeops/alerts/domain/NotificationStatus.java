package com.cognizant.storeops.alerts.domain;

/** Delivery state. FAILED is how a listener records a notification it could not create. */
public enum NotificationStatus {
    PENDING,
    SENT,
    READ,
    FAILED
}
