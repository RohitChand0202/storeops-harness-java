package com.cognizant.storeops.reports.repository;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.cognizant.storeops.reports.domain.Report;
import com.cognizant.storeops.reports.domain.ReportStatus;
import com.cognizant.storeops.reports.domain.ReportType;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryReportRepositoryTest {

    private static final Instant AT = Instant.parse("2026-09-20T08:00:00Z");

    private final InMemoryReportRepository repository = new InMemoryReportRepository();

    @Test
    void savesAndFindsByScope() {
        Report report = new Report("r-1", ReportType.DEPARTMENT_PERFORMANCE, "store-12", AT);
        report.setStatus(ReportStatus.FAILED);
        report.setPayload("{\"rows\":0}");
        repository.save(report);

        assertThat(repository.findById("r-1")).contains(report);
        assertThat(repository.findById("r-9")).isEmpty();
        assertThat(repository.findByScopeId("store-12")).containsExactly(report);
        assertThat(repository.findByScopeId("region-9")).isEmpty();
        assertThat(report.getStatus()).isEqualTo(ReportStatus.FAILED);
        assertThat(report.getPayload()).isEqualTo("{\"rows\":0}");
        assertThat(report.getId()).isEqualTo("r-1");
        assertThat(report.getGeneratedAt()).isEqualTo(AT);
    }
}
