package com.cognizant.storeops.shared.error;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Maps every error to the uniform body. An AppException keeps its own code and status; anything
 * else becomes a 500 with code INTERNAL_ERROR and no stack trace, because a client must never see
 * an internal class name.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles a typed application error.
     *
     * @param ex the error
     * @param request the request it arose from
     * @return the mapped response
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, WebRequest request) {
        return ResponseEntity.status(ex.httpStatus()).body(new ErrorResponse(
                ex.code(), ex.getMessage(), Instant.now(), path(request), List.of()));
    }

    /**
     * Handles a Bean Validation failure on a request body.
     *
     * @param ex the validation failure
     * @param request the request it arose from
     * @return a 400 carrying per-field detail
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "VALIDATION_FAILED", "Request body is not valid", Instant.now(),
                path(request), fields));
    }

    /**
     * Last resort. Logs the real cause and returns a generic body.
     *
     * @param ex anything not already handled
     * @param request the request it arose from
     * @return a 500 with code INTERNAL_ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        LOG.error("Unhandled exception on {}", path(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                "INTERNAL_ERROR", "Something went wrong", Instant.now(), path(request), List.of()));
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
