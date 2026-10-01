package com.cognizant.storeops.alerts.service;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.NotificationChannel;

/**
 * What the alerts service needs to raise a notification.
 *
 * @param userId recipient
 * @param alertType what it arose from
 * @param channel how it should reach them
 * @param payload rendered detail
 */
public record CreateNotificationCommand(
        String userId,
        AlertType alertType,
        NotificationChannel channel,
        String payload) {
}
