package com.cognizant.storeops.reports.repository;

import java.util.List;
import java.util.Optional;

import com.cognizant.storeops.reports.domain.Report;

/** Data access for reports. The only store the reports module writes to, per gate ARCH-4. */
public interface ReportRepository {

    /**
     * Finds one report.
     *
     * @param id report id
     * @return the report, or empty
     */
    Optional<Report> findById(String id);

    /**
     * Lists reports covering one store or region.
     *
     * @param scopeId store or region id
     * @return its reports
     */
    List<Report> findByScopeId(String scopeId);

    /**
     * Stores a report.
     *
     * @param report the report
     * @return the stored report
     */
    Report save(Report report);
}
