package com.cognizant.storeops.alerts.service.listener;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.NotificationChannel;
import com.cognizant.storeops.alerts.service.CreateNotificationCommand;
import com.cognizant.storeops.alerts.service.NotificationService;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.SlaBreachedEvent;
import com.cognizant.storeops.shared.event.TaskBlockedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationalEventListenerTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private NotificationService notificationService;
    private OperationalEventListener listener;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        listener = new OperationalEventListener(notificationService);
    }

    @Test
    void raisesAnSlaBreachAlertForTheAssignee() {
        listener.onSlaBreached(new SlaBreachedEvent("a-1", "user-7", "CRITICAL", "store-12", AT));

        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService, times(1)).create(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("user-7");
        assertThat(captor.getValue().alertType()).isEqualTo(AlertType.SLA_BREACH);
        assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(captor.getValue().payload()).contains("a-1").contains("CRITICAL");
    }

    @Test
    void raisesAnEscalationAlertWhenATaskIsBlocked() {
        listener.onTaskBlocked(new TaskBlockedEvent("a-2", "user-9", "pallet missing", AT));

        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService, times(1)).create(captor.capture());
        assertThat(captor.getValue().alertType()).isEqualTo(AlertType.ESCALATION);
        assertThat(captor.getValue().payload()).contains("pallet missing");
    }

    @Test
    void raisesAnAlertWhenAProgrammeCloses() {
        listener.onProgrammeClosed(new ProgrammeClosedEvent("prog-1", "store-12", "user-1", AT));

        verify(notificationService, times(1)).create(any());
    }

    @Test
    void dropsTheAlertWhenTheEventCarriesNoRecipient() {
        listener.onSlaBreached(new SlaBreachedEvent("a-1", null, "HIGH", "store-12", AT));

        verify(notificationService, never()).create(any());
    }

    @Test
    void swallowsATypedFailureSoThePublisherIsNotBrokenByAnAlert() {
        when(notificationService.create(any()))
                .thenThrow(new ConflictException("CONFLICT", "duplicate alert"));

        assertThatCode(() -> listener.onTaskBlocked(
                new TaskBlockedEvent("a-3", "user-9", "stuck", AT))).doesNotThrowAnyException();
    }
}
