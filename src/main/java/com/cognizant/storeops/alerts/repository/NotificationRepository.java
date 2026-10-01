package com.cognizant.storeops.alerts.repository;

import java.util.List;
import java.util.Optional;

import com.cognizant.storeops.alerts.domain.Notification;

/** Data access for alerts. Module-private under gate ARCH-1. */
public interface NotificationRepository {

    /**
     * Finds one notification.
     *
     * @param id notification id
     * @return the notification, or empty
     */
    Optional<Notification> findById(String id);

    /**
     * Lists notifications for one recipient.
     *
     * @param userId recipient
     * @return their notifications
     */
    List<Notification> findByUserId(String userId);

    /**
     * Stores a notification.
     *
     * @param notification the notification
     * @return the stored notification
     */
    Notification save(Notification notification);
}
