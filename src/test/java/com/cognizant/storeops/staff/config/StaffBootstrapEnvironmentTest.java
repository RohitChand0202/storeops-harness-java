package com.cognizant.storeops.staff.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.repository.InMemoryUserRepository;
import com.cognizant.storeops.staff.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the documented underscore environment variable names resolve at runtime. */
class StaffBootstrapEnvironmentTest {

    @Test
    void resolvesActorAndIncomingLeadFromEnvironmentVariables() {
        Instant now = Instant.parse("2026-10-12T10:00:00Z");
        Map<String, Object> environment = Map.ofEntries(
                Map.entry("STOREOPS_BOOTSTRAP_TOKEN", "manager-token"),
                Map.entry("STOREOPS_BOOTSTRAP_USER_ID", "manager-1"),
                Map.entry("STOREOPS_BOOTSTRAP_EMAIL", "manager@example.com"),
                Map.entry("STOREOPS_BOOTSTRAP_NAME", "Manager"),
                Map.entry("STOREOPS_BOOTSTRAP_ROLE", "STORE_MANAGER"),
                Map.entry("STOREOPS_BOOTSTRAP_STORE_ID", "store-12"),
                Map.entry("STOREOPS_BOOTSTRAP_REGION_ID", "region-1"),
                Map.entry("STOREOPS_BOOTSTRAP_TOKEN_TTL_SECONDS", "600"),
                Map.entry("STOREOPS_BOOTSTRAP_INCOMING_LEAD_TOKEN", "lead-token"),
                Map.entry("STOREOPS_BOOTSTRAP_INCOMING_LEAD_USER_ID", "lead-1"),
                Map.entry("STOREOPS_BOOTSTRAP_INCOMING_LEAD_EMAIL", "lead@example.com"),
                Map.entry("STOREOPS_BOOTSTRAP_INCOMING_LEAD_NAME", "Incoming Lead"));

        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("test-environment", environment)))
                .withBean(UserRepository.class, InMemoryUserRepository::new)
                .withBean(Clock.class, () -> Clock.fixed(now, ZoneOffset.UTC))
                .withUserConfiguration(StaffBootstrapConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    context.getBean(ApplicationRunner.class)
                            .run(new DefaultApplicationArguments(new String[0]));
                    UserRepository repository = context.getBean(UserRepository.class);
                    assertThat(repository.findById("manager-1")).isPresent();
                    assertThat(repository.findToken("manager-token").orElseThrow().expiresAt())
                            .isEqualTo(now.plusSeconds(600));
                    assertThat(repository.findById("lead-1").orElseThrow().role())
                            .isEqualTo(StaffRole.DEPARTMENT_LEAD);
                    assertThat(repository.findToken("lead-token").orElseThrow().userId())
                            .isEqualTo("lead-1");
                });
    }
}
