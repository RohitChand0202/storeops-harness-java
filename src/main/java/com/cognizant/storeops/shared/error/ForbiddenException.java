package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/** Valid user, insufficient StaffRole. */
public class ForbiddenException extends AppException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param code stable error code
     * @param message human-readable detail
     */
    public ForbiddenException(String code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }
}
