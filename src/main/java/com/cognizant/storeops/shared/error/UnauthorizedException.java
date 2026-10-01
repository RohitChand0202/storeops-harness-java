package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/** Missing or expired AuthToken. */
public class UnauthorizedException extends AppException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param code stable error code
     * @param message human-readable detail
     */
    public UnauthorizedException(String code, String message) {
        super(code, message, HttpStatus.UNAUTHORIZED);
    }
}
