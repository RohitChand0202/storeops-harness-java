package com.cognizant.storeops.shared.event;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SpringStoreOpsEventBusTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    @Test
    void publishesExactlyOnceThroughSpring() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        StoreOpsEventBus bus = new SpringStoreOpsEventBus(publisher);
        TaskBlockedEvent event = new TaskBlockedEvent("a-1", "user-7", "stuck", AT);

        bus.publish(event);

        verify(publisher, times(1)).publishEvent(event);
    }

    @Test
    void everyCatalogueEventNamesItsTypeAndCarriesItsPayload() {
        assertThat(new SlaBreachedEvent("a-1", "user-7", "HIGH", "store-12", AT).eventType())
                .isEqualTo("SLA_BREACHED");
        assertThat(new TaskBlockedEvent("a-1", "user-7", "stuck", AT).eventType())
                .isEqualTo("TASK_BLOCKED");
        assertThat(new ProgrammeClosedEvent("prog-1", "store-12", "user-1", AT).eventType())
                .isEqualTo("PROGRAMME_CLOSED");
        assertThat(new RegionalRollupRequestedEvent("region-1", "user-1", AT).eventType())
                .isEqualTo("REGIONAL_ROLLUP_REQUESTED");

        SlaBreachedEvent sla = new SlaBreachedEvent("a-1", "user-7", "HIGH", "store-12", AT);
        assertThat(sla.taskId()).isEqualTo("a-1");
        assertThat(sla.assigneeId()).isEqualTo("user-7");
        assertThat(sla.priority()).isEqualTo("HIGH");
        assertThat(sla.storeId()).isEqualTo("store-12");
        assertThat(sla.occurredAt()).isEqualTo(AT);

        RegionalRollupRequestedEvent rollup = new RegionalRollupRequestedEvent("region-1", "user-1", AT);
        assertThat(rollup.regionId()).isEqualTo("region-1");
        assertThat(rollup.requestedByUserId()).isEqualTo("user-1");
    }
}
