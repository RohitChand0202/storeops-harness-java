package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Base of the typed error hierarchy. Every error the API surfaces carries a stable code a client
 * can branch on, a human-readable message, and the HTTP status it maps to.
 *
 * <p>Gate ERR-1: no class in a service or web package may throw a raw RuntimeException,
 * IllegalStateException, IllegalArgumentException, UnsupportedOperationException or bare Exception.
 */
public abstract class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final HttpStatus httpStatus;

    /**
     * Creates a typed error.
     *
     * @param code stable SCREAMING_SNAKE code, part of the API contract
     * @param message human-readable detail
     * @param httpStatus status GlobalExceptionHandler maps this to
     */
    protected AppException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    /**
     * Stable error code.
     *
     * @return the code
     */
    public String code() {
        return code;
    }

    /**
     * Status this error maps to.
     *
     * @return the HTTP status
     */
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
