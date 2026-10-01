package com.cognizant.storeops.alerts.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.Notification;
import com.cognizant.storeops.alerts.domain.NotificationChannel;
import com.cognizant.storeops.alerts.domain.NotificationStatus;
import com.cognizant.storeops.alerts.repository.NotificationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private NotificationRepository notificationRepository;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        service = new NotificationServiceImpl(notificationRepository, Clock.fixed(AT, ZoneOffset.UTC));
    }

    @Test
    void listsForUser() {
        Notification notification = new Notification("n-1", "user-7", AlertType.SLA_BREACH,
                NotificationChannel.IN_APP, "late", AT);
        when(notificationRepository.findByUserId("user-7")).thenReturn(List.of(notification));

        assertThat(service.listForUser("user-7")).containsExactly(notification);
    }

    @Test
    void createsPendingNotificationAndPersistsItOnce() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification created = service.create(new CreateNotificationCommand(
                "user-7", AlertType.SHIFT_HANDOVER, NotificationChannel.IN_APP, "handover done"));

        assertThat(created.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(created.getUserId()).isEqualTo("user-7");
        assertThat(created.getAlertType()).isEqualTo(AlertType.SHIFT_HANDOVER);
        assertThat(created.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(created.getPayload()).isEqualTo("handover done");
        assertThat(created.getCreatedAt()).isEqualTo(AT);
        verify(notificationRepository, times(1)).save(created);
    }

    @Test
    void marksANotificationFailed() {
        Notification notification = new Notification("n-1", "user-7", AlertType.ESCALATION,
                NotificationChannel.EMAIL, "blocked", AT);

        notification.setStatus(NotificationStatus.FAILED);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getId()).isEqualTo("n-1");
    }
}
