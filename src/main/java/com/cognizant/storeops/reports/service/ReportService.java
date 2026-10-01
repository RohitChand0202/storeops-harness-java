package com.cognizant.storeops.reports.service;

import java.util.List;

import com.cognizant.storeops.reports.domain.Report;
import com.cognizant.storeops.reports.domain.ReportType;

/**
 * Report business logic.
 *
 * <p>Gate ARCH-2 forbids every class outside the reports module from depending on this package.
 * Reports is a pure consumer: it is reached by event, and it reaches other modules only through
 * their service interfaces, never their repositories.
 */
public interface ReportService {

    /**
     * Lists reports covering one store or region.
     *
     * @param scopeId store or region id
     * @return its reports
     */
    List<Report> listForScope(String scopeId);

    /**
     * Requests a report.
     *
     * @param reportType what it should cover
     * @param scopeId store or region to cover
     * @return the pending report
     */
    Report request(ReportType reportType, String scopeId);
}
