package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/** Malformed input that Bean Validation did not catch. */
public class ValidationException extends AppException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param code stable error code
     * @param message human-readable detail
     */
    public ValidationException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
