package com.cognizant.storeops.shared.error;

import org.springframework.http.HttpStatus;

/** Entity id does not resolve. */
public class NotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the error.
     *
     * @param code stable error code
     * @param message human-readable detail
     */
    public NotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
