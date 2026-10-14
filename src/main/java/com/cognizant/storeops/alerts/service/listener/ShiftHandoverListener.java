package com.cognizant.storeops.alerts.service.listener;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.NotificationChannel;
import com.cognizant.storeops.alerts.service.CreateNotificationCommand;
import com.cognizant.storeops.alerts.service.NotificationService;
import com.cognizant.storeops.shared.error.AppException;
import com.cognizant.storeops.shared.event.ShiftHandoverCompletedEvent;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.service.StaffService;

/**
 * Delivers one in-app handover alert to an active department lead in the store. The event keeps
 * activities independent of alerts, and the lookup uses the staff service's read-only boundary.
 */
@Component
public class ShiftHandoverListener {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftHandoverListener.class);

    private final StaffService staffService;
    private final NotificationService notificationService;

    /**
     * Creates the listener.
     *
     * @param staffService read-only staff lookup
     * @param notificationService alert writer within this module
     */
    public ShiftHandoverListener(StaffService staffService, NotificationService notificationService) {
        this.staffService = staffService;
        this.notificationService = notificationService;
    }

    /**
     * Creates one alert for a completed handover. Alert failures do not undo completed activities.
     *
     * @param event completed handover
     */
    @EventListener
    public void on(ShiftHandoverCompletedEvent event) {
        try {
            User recipient = staffService.findDepartmentLeadsForStore(event.storeId()).stream()
                    .filter(User::active)
                    .filter(user -> !user.id().equals(event.handoverByUserId()))
                    .min(Comparator.comparing(User::id))
                    .orElse(null);
            if (recipient == null) {
                LOG.warn("No incoming department lead available for store {}", event.storeId());
                return;
            }
            notificationService.create(new CreateNotificationCommand(recipient.id(),
                    AlertType.SHIFT_HANDOVER, NotificationChannel.IN_APP,
                    "Shift handover completed for store " + event.storeId() + ": "
                            + event.updatedTaskIds().size() + " activities updated"));
        } catch (AppException ex) {
            LOG.error("Could not create shift handover alert for store {}: {}", event.storeId(), ex.code());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            LOG.error("Unexpected handover alert failure for store {}", event.storeId(), ex);
        }
    }
}
