package com.cognizant.storeops.staff.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.UnauthorizedException;
import com.cognizant.storeops.staff.domain.AuthToken;
import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-09-20T08:00:00Z");

    private UserRepository userRepository;
    private StaffServiceImpl service;

    private User user() {
        return new User("user-7", "lead@example.com", "Dept Lead", StaffRole.DEPARTMENT_LEAD,
                "store-12", "region-1", true);
    }

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new StaffServiceImpl(userRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void findsUserById() {
        when(userRepository.findById("user-7")).thenReturn(Optional.of(user()));

        assertThat(service.findById("user-7").email()).isEqualTo("lead@example.com");
    }

    @Test
    void throwsTypedNotFoundForUnknownUser() {
        when(userRepository.findById("user-9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("user-9")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void resolvesAValidTokenToThePrincipal() {
        when(userRepository.findToken("tok")).thenReturn(
                Optional.of(new AuthToken("tok", "user-7", NOW.plusSeconds(3600))));
        when(userRepository.findById("user-7")).thenReturn(Optional.of(user()));

        StoreOpsUser principal = service.resolveToken("tok");

        assertThat(principal.id()).isEqualTo("user-7");
        assertThat(principal.storeId()).isEqualTo("store-12");
        assertThat(principal.regionId()).isEqualTo("region-1");
        assertThat(principal.role()).isEqualTo("DEPARTMENT_LEAD");
    }

    @Test
    void rejectsAnUnknownToken() {
        when(userRepository.findToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveToken("nope"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not recognised");
    }

    @Test
    void rejectsAnExpiredToken() {
        when(userRepository.findToken("old")).thenReturn(
                Optional.of(new AuthToken("old", "user-7", NOW.minusSeconds(1))));

        assertThatThrownBy(() -> service.resolveToken("old"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }
}
