package com.cognizant.storeops.alerts.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cognizant.storeops.alerts.domain.Notification;
import com.cognizant.storeops.alerts.repository.NotificationRepository;

/** All alert business logic. */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param notificationRepository alert store
     * @param clock injected clock
     */
    public NotificationServiceImpl(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Override
    public List<Notification> listForUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public Notification create(CreateNotificationCommand command) {
        Notification notification = new Notification(UUID.randomUUID().toString(), command.userId(),
                command.alertType(), command.channel(), command.payload(), clock.instant());
        return notificationRepository.save(notification);
    }
}
