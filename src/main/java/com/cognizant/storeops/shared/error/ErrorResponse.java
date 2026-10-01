package com.cognizant.storeops.shared.error;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body. Built only by GlobalExceptionHandler, never hand-built in a controller.
 *
 * @param code stable error code
 * @param message human-readable detail
 * @param timestamp when the error was produced
 * @param path request path
 * @param fieldErrors per-field detail for a Bean Validation failure, empty otherwise
 */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        List<FieldError> fieldErrors) {

    /** Canonical constructor, defensively copying the field error list. */
    public ErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    /**
     * One rejected field.
     *
     * @param field property name
     * @param message why it was rejected
     */
    public record FieldError(String field, String message) {
    }
}
