package com.cognizant.storeops.reports.service.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.cognizant.storeops.reports.domain.ReportType;
import com.cognizant.storeops.reports.service.ReportService;
import com.cognizant.storeops.shared.event.ProgrammeClosedEvent;
import com.cognizant.storeops.shared.event.RegionalRollupRequestedEvent;

/**
 * Builds reports in response to operational events. Reports consumes events; it never publishes one
 * that causes a write elsewhere, which is the half of ARCH-4 that ArchUnit cannot prove.
 */
@Component
public class ProgrammeEventListener {

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
        reportService.request(ReportType.STORE_SUMMARY, event.storeId());
    }

    /**
     * Builds a regional rollup when one is requested.
     *
     * @param event the request
     */
    @EventListener
    public void onRollupRequested(RegionalRollupRequestedEvent event) {
        reportService.request(ReportType.REGIONAL_ROLLUP, event.regionId());
    }
}
