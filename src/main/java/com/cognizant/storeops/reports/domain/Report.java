package com.cognizant.storeops.reports.domain;

import java.time.Instant;

/**
 * A generated performance summary. The reports module is read-only across the whole system: it
 * aggregates through other modules' service interfaces and writes only to its own store, which is
 * what gate ARCH-4 enforces.
 */
public class Report {

    private final String id;
    private final ReportType reportType;
    private final String scopeId;
    private final Instant generatedAt;
    private ReportStatus status;
    private String payload;

    /**
     * Creates a pending report.
     *
     * @param id stable id
     * @param reportType what it covers
     * @param scopeId store or region it covers
     * @param generatedAt when it was requested
     */
    public Report(String id, ReportType reportType, String scopeId, Instant generatedAt) {
        this.id = id;
        this.reportType = reportType;
        this.scopeId = scopeId;
        this.generatedAt = generatedAt;
        this.status = ReportStatus.PENDING;
    }

    /**
     * Report id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * What the report covers.
     *
     * @return the report type
     */
    public ReportType getReportType() {
        return reportType;
    }

    /**
     * Store or region covered.
     *
     * @return the scope id
     */
    public String getScopeId() {
        return scopeId;
    }

    /**
     * When it was requested.
     *
     * @return the generation instant
     */
    public Instant getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Whether it has been built.
     *
     * @return the status
     */
    public ReportStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    /**
     * Rendered content.
     *
     * @return the payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the rendered content.
     *
     * @param payload the new payload
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }
}
