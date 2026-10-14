package com.cognizant.storeops.activities.service;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cognizant.storeops.activities.ActivityFixtures;
import com.cognizant.storeops.activities.domain.TaskStatus;
import com.cognizant.storeops.activities.repository.InMemoryAuditEntryRepository;
import com.cognizant.storeops.activities.repository.InMemoryTaskRepository;
import com.cognizant.storeops.alerts.service.NotificationService;
import com.cognizant.storeops.alerts.service.listener.ShiftHandoverListener;
import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.ConflictException;
import com.cognizant.storeops.shared.event.ShiftHandoverCompletedEvent;
import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.service.StaffService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShiftHandoverIntegrationTest {

    private InMemoryTaskRepository taskRepository;
    private InMemoryAuditEntryRepository auditRepository;
    private StaffService staffService;
    private NotificationService notificationService;
    private ActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        auditRepository = new InMemoryAuditEntryRepository();
        staffService = mock(StaffService.class);
        notificationService = mock(NotificationService.class);
        ShiftHandoverListener listener = new ShiftHandoverListener(staffService, notificationService);
        service = new ActivityServiceImpl(taskRepository, auditRepository,
                event -> listener.on((ShiftHandoverCompletedEvent) event),
                Clock.fixed(ActivityFixtures.AT, ZoneOffset.UTC));
        taskRepository.save(ActivityFixtures.task("a-1", TaskStatus.TODO));
    }

    @Test
    void missingLeadDoesNotReverseAnAppliedTransitionOrAudit() {
        when(staffService.findDepartmentLeadsForStore("store-12")).thenReturn(List.of());

        BulkStatusResult result = service.bulkUpdateStatus(command());

        assertThat(result.succeeded()).hasSize(1);
        assertThat(taskRepository.findById("a-1").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(auditRepository.findByTaskId("a-1")).hasSize(1);
        verify(notificationService, never()).create(any());
    }

    @Test
    void failedAlertDoesNotReverseAnAppliedTransitionOrAudit() {
        when(staffService.findDepartmentLeadsForStore("store-12"))
                .thenReturn(List.of(new User("lead-9", "lead@example.test", "Lead",
                        StaffRole.DEPARTMENT_LEAD, "store-12", "region-1", true)));
        when(notificationService.create(any()))
                .thenThrow(new ConflictException("CONFLICT", "Notification write failed"));

        BulkStatusResult result = service.bulkUpdateStatus(command());

        assertThat(result.succeeded()).extracting(BulkStatusResult.UpdatedItem::taskId)
                .containsExactly("a-1");
        assertThat(taskRepository.findById("a-1").orElseThrow().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(auditRepository.findByTaskId("a-1")).hasSize(1);
        verify(notificationService).create(any());
    }

    private BulkStatusCommand command() {
        return new BulkStatusCommand(
                new StoreOpsUser("user-7", "store-12", "region-1", "ASSOCIATE"),
                List.of(new BulkStatusCommand.ItemUpdate("a-1", TaskStatus.DONE, null)));
    }
}
