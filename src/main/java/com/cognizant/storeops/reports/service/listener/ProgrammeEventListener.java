package com.cognizant.storeops.reports.service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.cognizant.storeops.reports.domain.ReportType;
import com.cognizant.storeops.reports.service.ReportService;
import com.cognizant.storeops.shared.error.AppException;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.RegionalRollupRequestedEvent;

/**
 * Builds reports in response to operational events. Reports consumes events; it never publishes one
 * that causes a write elsewhere, which is the half of ARCH-4 that ArchUnit cannot prove.
 */
@Component
public class ProgrammeEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProgrammeEventListener.class);

    private final ReportService reportService;

    /**
     * Creates the listener.
     *
     * @param reportService reports service, in this module
     */
    public ProgrammeEventListener(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Builds a store summary when a programme closes.
     *
     * @param event the closure
     */
    @EventListener
    public void onProgrammeClosed(ProgrammeClosedEvent event) {
        requestReport(ReportType.STORE_SUMMARY, event.storeId());
    }

    /**
     * Builds a regional rollup when one is requested.
     *
     * @param event the request
     */
    @EventListener
    public void onRollupRequested(RegionalRollupRequestedEvent event) {
        requestReport(ReportType.REGIONAL_ROLLUP, event.regionId());
    }

    private void requestReport(ReportType type, String scopeId) {
        try {
            reportService.request(type, scopeId);
        } catch (AppException ex) {
            LOG.error("Report request failed for type {} and scope {}: {}", type, scopeId, ex.code(), ex);
        }
    }
}
