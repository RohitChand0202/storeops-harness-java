package com.cognizant.storeops.shared.error;

/** Maps anything that is not an AppException to a generic code. */
public class GlobalExceptionHandler {

    /** Runs the action and maps a failure to a stable code. */
    public String handle(Runnable action) {
        try {
            action.run();
            return "OK";
        } catch (Exception e) {
            return "INTERNAL_ERROR";
        }
    }
}
