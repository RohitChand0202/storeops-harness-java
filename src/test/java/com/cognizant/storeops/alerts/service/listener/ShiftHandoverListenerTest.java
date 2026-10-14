package com.cognizant.storeops.alerts.service.listener;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cognizant.storeops.alerts.domain.AlertType;
import com.cognizant.storeops.alerts.domain.Notification;
import com.cognizant.storeops.alerts.domain.NotificationChannel;
import com.cognizant.storeops.alerts.repository.InMemoryNotificationRepository;
import com.cognizant.storeops.alerts.service.CreateNotificationCommand;
import com.cognizant.storeops.alerts.service.NotificationService;
import com.cognizant.storeops.alerts.service.NotificationServiceImpl;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.event.ShiftHandoverCompletedEvent;
import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.service.StaffService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShiftHandoverListenerTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private StaffService staffService;
    private NotificationService notificationService;
    private ShiftHandoverListener listener;

    @BeforeEach
    void setUp() {
        staffService = mock(StaffService.class);
        notificationService = mock(NotificationService.class);
        listener = new ShiftHandoverListener(staffService, notificationService);
    }

    @Test
    void createsOneNotificationForAnActiveLeadOtherThanTheOutgoingStaffMember() {
        when(staffService.findDepartmentLeadsForStore("store-12")).thenReturn(List.of(
                lead("user-7", true), lead("user-9", true), lead("user-8", false)));

        listener.on(event());

        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService, times(1)).create(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo("user-9");
        assertThat(captor.getValue().alertType()).isEqualTo(AlertType.SHIFT_HANDOVER);
        assertThat(captor.getValue().channel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(captor.getValue().payload()).contains("store-12").contains("2 activities");
    }

    @Test
    void persistsExactlyOneIncomingLeadAlertForACompletedHandover() {
        InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
        NotificationService realNotifications = new NotificationServiceImpl(repository,
                Clock.fixed(AT, ZoneOffset.UTC));
        ShiftHandoverListener persistentListener = new ShiftHandoverListener(staffService,
                realNotifications);
        when(staffService.findDepartmentLeadsForStore("store-12"))
                .thenReturn(List.of(lead("user-7", true), lead("user-9", true)));

        persistentListener.on(event());

        List<Notification> stored = repository.findByUserId("user-9");
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getAlertType()).isEqualTo(AlertType.SHIFT_HANDOVER);
        assertThat(stored.get(0).getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(stored.get(0).getPayload()).contains("store-12").contains("2 activities");
        assertThat(stored.get(0).getCreatedAt()).isEqualTo(AT);
        assertThat(repository.findByUserId("user-7")).isEmpty();
    }

    @Test
    void dropsAlertWhenNoIncomingLeadIsAvailable() {
        when(staffService.findDepartmentLeadsForStore("store-12"))
                .thenReturn(List.of(lead("user-7", true)));

        listener.on(event());

        verify(notificationService, never()).create(any());
    }

    @Test
    void notificationFailureDoesNotUndoTheCompletedHandover() {
        when(staffService.findDepartmentLeadsForStore("store-12"))
                .thenReturn(List.of(lead("user-9", true)));
        when(notificationService.create(any()))
                .thenThrow(new ConflictException("CONFLICT", "notification write failed"));

        assertThatCode(() -> listener.on(event())).doesNotThrowAnyException();
    }

    @Test
    void unexpectedAlertFailureIsIsolatedFromTheCompletedHandover() {
        when(staffService.findDepartmentLeadsForStore("store-12"))
                .thenReturn(List.of(lead("user-9", true)));
        when(notificationService.create(any()))
                .thenThrow(new IllegalStateException("notification store unavailable"));

        assertThatCode(() -> listener.on(event())).doesNotThrowAnyException();
    }

    private ShiftHandoverCompletedEvent event() {
        return new ShiftHandoverCompletedEvent("store-12", "user-7", List.of("a-1", "a-2"), AT);
    }

    private User lead(String id, boolean active) {
        return new User(id, id + "@example.test", id, StaffRole.DEPARTMENT_LEAD,
                "store-12", "region-1", active);
    }
}
