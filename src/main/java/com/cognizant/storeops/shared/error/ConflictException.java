package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/** Concurrent modification or duplicate creation. */
public class ConflictException extends AppException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param code stable error code
     * @param message human-readable detail
     */
    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
