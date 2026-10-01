package com.cognizant.storeops.alerts.repository;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.Notification;
import com.cognizant.storeops.alerts.domain.NotificationChannel;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryNotificationRepositoryTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private final InMemoryNotificationRepository repository = new InMemoryNotificationRepository();

    @Test
    void savesAndFindsByUser() {
        Notification mine = new Notification("n-1", "user-7", AlertType.SLA_BREACH,
                NotificationChannel.IN_APP, "late", AT);
        Notification theirs = new Notification("n-2", "user-8", AlertType.INVENTORY,
                NotificationChannel.EMAIL, "stock", AT);
        repository.save(mine);
        repository.save(theirs);

        assertThat(repository.findById("n-1")).contains(mine);
        assertThat(repository.findById("n-9")).isEmpty();
        assertThat(repository.findByUserId("user-7")).containsExactly(mine);
        assertThat(repository.findByUserId("user-0")).isEmpty();
    }
}
