package com.cognizant.storeops.reports.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.cognizant.storeops.reports.domain.Report;
import com.cognizant.storeops.reports.domain.ReportStatus;
import com.cognizant.storeops.reports.domain.ReportType;
import com.cognizant.storeops.reports.repository.ReportRepository;

/** All report business logic. Writes only to ReportRepository. */
@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param reportRepository report store, the only store reports writes to
     * @param clock injected clock
     */
    public ReportServiceImpl(ReportRepository reportRepository, Clock clock) {
        this.reportRepository = reportRepository;
        this.clock = clock;
    }

    @Override
    public List<Report> listForScope(String scopeId) {
        return reportRepository.findByScopeId(scopeId);
    }

    @Override
    public Report request(ReportType reportType, String scopeId) {
        Report report = new Report(UUID.randomUUID().toString(), reportType, scopeId, clock.instant());
        report.setStatus(ReportStatus.READY);
        report.setPayload("{}");
        return reportRepository.save(report);
    }
}
