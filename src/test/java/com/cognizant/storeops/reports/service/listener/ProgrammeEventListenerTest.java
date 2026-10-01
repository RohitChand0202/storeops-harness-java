package com.cognizant.storeops.reports.service.listener;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cognizant.storeops.reports.domain.ReportType;
import com.cognizant.storeops.reports.service.ReportService;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.RegionalRollupRequestedEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProgrammeEventListenerTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private ReportService reportService;
    private ProgrammeEventListener listener;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);
        listener = new ProgrammeEventListener(reportService);
    }

    @Test
    void buildsAStoreSummaryWhenAProgrammeCloses() {
        listener.onProgrammeClosed(new ProgrammeClosedEvent("prog-1", "store-12", "user-1", AT));

        verify(reportService, times(1)).request(ReportType.STORE_SUMMARY, "store-12");
    }

    @Test
    void buildsARegionalRollupWhenOneIsRequested() {
        listener.onRollupRequested(new RegionalRollupRequestedEvent("region-1", "user-1", AT));

        verify(reportService, times(1)).request(ReportType.REGIONAL_ROLLUP, "region-1");
    }
}
