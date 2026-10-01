package com.cognizant.storeops.alerts.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.cognizant.storeops.alerts.domain.Notification;

/** In-memory alert store. */
@Repository
public class InMemoryNotificationRepository implements NotificationRepository {

    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();

    @Override
    public Optional<Notification> findById(String id) {
        return Optional.ofNullable(notifications.get(id));
    }

    @Override
    public List<Notification> findByUserId(String userId) {
        List<Notification> found = new ArrayList<>();
        for (Notification notification : notifications.values()) {
            if (notification.getUserId().equals(userId)) {
                found.add(notification);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public Notification save(Notification notification) {
        notifications.put(notification.getId(), notification);
        return notification;
    }
}
