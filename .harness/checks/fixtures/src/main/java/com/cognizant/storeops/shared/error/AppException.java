package com.cognizant.storeops.shared.error;

/** Base typed error. Extends RuntimeException by design; shared.error is exempt from ERR-1. */
public abstract class AppException extends RuntimeException {

    private final String code;

    protected AppException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Stable error code. */
    public String code() {
        return code;
    }
}
