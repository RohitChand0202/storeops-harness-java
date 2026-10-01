package com.cognizant.storeops.shared.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AppExceptionTest {

    @Test
    void everySubclassCarriesItsAgreedStatus() {
        assertThat(new ValidationException("VALIDATION_FAILED", "m").httpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(new UnauthorizedException("UNAUTHORIZED", "m").httpStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(new ForbiddenException("FORBIDDEN", "m").httpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(new NotFoundException("NOT_FOUND", "m").httpStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(new ConflictException("CONFLICT", "m").httpStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(new RuleViolationException("INVALID_TRANSITION", "m").httpStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void carriesTheCodeAndMessage() {
        NotFoundException ex = new NotFoundException("NOT_FOUND", "Activity a-1 does not exist");

        assertThat(ex.code()).isEqualTo("NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("Activity a-1 does not exist");
    }
}
