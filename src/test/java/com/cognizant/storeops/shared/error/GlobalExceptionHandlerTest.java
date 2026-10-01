package com.cognizant.storeops.shared.error;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest requestFor(String path) {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + path);
        return request;
    }

    @Test
    void keepsTheCodeAndStatusOfATypedError() {
        ResponseEntity<ErrorResponse> response = handler.handleAppException(
                new RuleViolationException("INVALID_TRANSITION", "cannot move"),
                requestFor("/api/activities/a-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_TRANSITION");
        assertThat(response.getBody().message()).isEqualTo("cannot move");
        assertThat(response.getBody().path()).isEqualTo("/api/activities/a-1");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void reportsEveryRejectedFieldOnAValidationFailure() throws Exception {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.rejectValue(null, "NotBlank", "must not be blank");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod("stub"), -1), binding);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, requestFor("/api/activities"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void hidesTheInternalCauseBehindAGenericCode() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("connection pool exhausted"), requestFor("/api/activities"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("connection pool");
    }

    @Test
    void defendsTheFieldErrorListAgainstNullAndMutation() {
        ErrorResponse withNull = new ErrorResponse("C", "m", null, "/p", null);
        assertThat(withNull.fieldErrors()).isEmpty();

        List<ErrorResponse.FieldError> source = new java.util.ArrayList<>();
        source.add(new ErrorResponse.FieldError("title", "must not be blank"));
        ErrorResponse copied = new ErrorResponse("C", "m", null, "/p", source);
        source.clear();

        assertThat(copied.fieldErrors()).hasSize(1);
        assertThat(copied.fieldErrors().get(0).field()).isEqualTo("title");
    }

    private void stub() {
    }
}
