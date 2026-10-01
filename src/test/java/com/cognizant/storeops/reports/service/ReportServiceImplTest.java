package com.cognizant.storeops.reports.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cognizant.storeops.reports.domain.Report;
import com.cognizant.storeops.reports.domain.ReportStatus;
import com.cognizant.storeops.reports.domain.ReportType;
import com.cognizant.storeops.reports.repository.ReportRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private ReportRepository reportRepository;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        service = new ReportServiceImpl(reportRepository, Clock.fixed(AT, ZoneOffset.UTC));
    }

    @Test
    void listsForScope() {
        Report report = new Report("r-1", ReportType.STORE_SUMMARY, "store-12", AT);
        when(reportRepository.findByScopeId("store-12")).thenReturn(List.of(report));

        assertThat(service.listForScope("store-12")).containsExactly(report);
    }

    @Test
    void requestsAReportAndWritesOnlyToItsOwnStore() {
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Report requested = service.request(ReportType.REGIONAL_ROLLUP, "region-1");

        assertThat(requested.getReportType()).isEqualTo(ReportType.REGIONAL_ROLLUP);
        assertThat(requested.getScopeId()).isEqualTo("region-1");
        assertThat(requested.getStatus()).isEqualTo(ReportStatus.READY);
        assertThat(requested.getPayload()).isEqualTo("{}");
        assertThat(requested.getGeneratedAt()).isEqualTo(AT);
        verify(reportRepository, times(1)).save(requested);
    }
}
