package com.cognizant.storeops.alerts.service;

import java.util.List;

import com.cognizant.storeops.alerts.domain.Notification;

/**
 * Alert business logic.
 *
 * <p>Gate ARCH-2 forbids every class outside the alerts module from depending on this package, in
 * either direction, reads included. A module that wants a notification raised publishes an event
 * and a listener in this module consumes it.
 */
public interface NotificationService {

    /**
     * Lists notifications for one recipient.
     *
     * @param userId recipient
     * @return their notifications
     */
    List<Notification> listForUser(String userId);

    /**
     * Raises a notification.
     *
     * @param command what to raise
     * @return the created notification
     */
    Notification create(CreateNotificationCommand command);
}
