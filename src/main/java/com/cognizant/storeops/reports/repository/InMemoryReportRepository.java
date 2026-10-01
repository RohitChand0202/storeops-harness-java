package com.cognizant.storeops.reports.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.cognizant.storeops.reports.domain.Report;

/** In-memory report store. */
@Repository
public class InMemoryReportRepository implements ReportRepository {

    private final Map<String, Report> reports = new ConcurrentHashMap<>();

    @Override
    public Optional<Report> findById(String id) {
        return Optional.ofNullable(reports.get(id));
    }

    @Override
    public List<Report> findByScopeId(String scopeId) {
        List<Report> found = new ArrayList<>();
        for (Report report : reports.values()) {
            if (report.getScopeId().equals(scopeId)) {
                found.add(report);
            }
        }
        return List.copyOf(found);
    }

    @Override
    public Report save(Report report) {
        reports.put(report.getId(), report);
        return report;
    }
}
