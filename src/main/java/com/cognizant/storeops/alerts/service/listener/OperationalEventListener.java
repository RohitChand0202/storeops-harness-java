package com.cognizant.storeops.alerts.service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.NotificationChannel;
import com.cognizant.storeops.alerts.service.CreateNotificationCommand;
import com.cognizant.storeops.alerts.service.NotificationService;
import com.cognizant.storeops.shared.error.AppException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.SlaBreachedEvent;
import com.cognizant.storeops.shared.event.TaskBlockedEvent;

/**
 * Turns operational events into alerts. The listener lives in the consuming module, which is the
 * whole point of ARCH-2: activities has no dependency on alerts and does not know a notification
 * results from a task going overdue.
 *
 * <p>Delivery is synchronous and in-process, so a listener that throws surfaces in the publisher's
 * call stack. A bulk handover must not fail because one notification could not be created, so each
 * handler catches AppException and logs it rather than propagating.
 */
@Component
public class OperationalEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(OperationalEventListener.class);

    private final NotificationService notificationService;

    /**
     * Creates the listener.
     *
     * @param notificationService alerts service, in this module, so no boundary is crossed
     */
    public OperationalEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Raises an SLA breach alert for the assignee.
     *
     * @param event the breach
     */
    @EventListener
    public void onSlaBreached(SlaBreachedEvent event) {
        raise(event.assigneeId(), AlertType.SLA_BREACH,
                "Task " + event.taskId() + " breached its SLA at priority " + event.priority());
    }

    /**
     * Raises an escalation alert when a task is blocked.
     *
     * @param event the blocking
     */
    @EventListener
    public void onTaskBlocked(TaskBlockedEvent event) {
        raise(event.blockedByUserId(), AlertType.ESCALATION,
                "Task " + event.taskId() + " was blocked: " + event.reason());
    }

    /**
     * Raises an inventory alert when a programme closes.
     *
     * @param event the closure
     */
    @EventListener
    public void onProgrammeClosed(ProgrammeClosedEvent event) {
        raise(event.closedByUserId(), AlertType.INVENTORY,
                "Programme " + event.programmeId() + " closed");
    }

    private void raise(String recipientId, AlertType type, String payload) {
        if (recipientId == null) {
            LOG.warn("Dropping {} alert: event carried no recipient", type);
            return;
        }
        try {
            notificationService.create(new CreateNotificationCommand(
                    recipientId, type, NotificationChannel.IN_APP, payload));
        } catch (AppException e) {
            LOG.error("Could not create {} alert for {}: {}", type, recipientId, e.code(), e);
        }
    }
}
