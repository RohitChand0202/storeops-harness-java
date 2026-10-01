package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/** A business rule refused a well-formed request. */
public class RuleViolationException extends AppException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param code stable error code
     * @param message human-readable detail
     */
    public RuleViolationException(String code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
