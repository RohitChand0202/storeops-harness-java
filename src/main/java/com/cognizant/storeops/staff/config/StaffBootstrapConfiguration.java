package com.cognizant.storeops.staff.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cognizant.storeops.shared.error.ValidationException;
import com.cognizant.storeops.staff.domain.AuthToken;
import com.cognizant.storeops.staff.domain.StaffRole;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.repository.UserRepository;

/** Optional account for a local demonstration, enabled only with an explicit token property. */
@Configuration
public class StaffBootstrapConfiguration {

    @Bean
    @ConditionalOnProperty(name = "storeops.bootstrap.token")
    ApplicationRunner bootstrapStaff(UserRepository repository, Clock clock,
            @Value("${storeops.bootstrap.token}") String token,
            @Value("${storeops.bootstrap.user-id}") String userId,
            @Value("${storeops.bootstrap.email}") String email,
            @Value("${storeops.bootstrap.name}") String name,
            @Value("${storeops.bootstrap.role}") StaffRole role,
            @Value("${storeops.bootstrap.store-id}") String storeId,
            @Value("${storeops.bootstrap.region-id}") String regionId,
            @Value("${storeops.bootstrap.token-ttl-seconds:28800}") long tokenTtlSeconds,
            @Value("${storeops.bootstrap.incoming-lead.token:}") String incomingLeadToken,
            @Value("${storeops.bootstrap.incoming-lead.user-id:}") String incomingLeadId,
            @Value("${storeops.bootstrap.incoming-lead.email:}") String incomingLeadEmail,
            @Value("${storeops.bootstrap.incoming-lead.name:}") String incomingLeadName) {
        return args -> {
            if (token.isBlank() || userId.isBlank() || storeId.isBlank()
                    || regionId.isBlank() || email.isBlank() || name.isBlank()
                    || tokenTtlSeconds <= 0) {
                throw new ValidationException("INVALID_BOOTSTRAP_CONFIGURATION",
                        "Bootstrap user properties must be nonblank and TTL positive");
            }
            repository.save(new User(userId, email, name, role, storeId, regionId, true));
            repository.saveToken(new AuthToken(token, userId,
                    clock.instant().plusSeconds(tokenTtlSeconds)));

            boolean leadConfigured = !incomingLeadToken.isBlank() || !incomingLeadId.isBlank()
                    || !incomingLeadEmail.isBlank() || !incomingLeadName.isBlank();
            if (leadConfigured) {
                if (incomingLeadToken.isBlank() || incomingLeadId.isBlank()
                        || incomingLeadEmail.isBlank() || incomingLeadName.isBlank()
                        || incomingLeadToken.equals(token) || incomingLeadId.equals(userId)) {
                    throw new ValidationException("INVALID_BOOTSTRAP_CONFIGURATION",
                            "Incoming lead needs a distinct token and complete account properties");
                }
                repository.save(new User(incomingLeadId, incomingLeadEmail, incomingLeadName,
                        StaffRole.DEPARTMENT_LEAD, storeId, regionId, true));
                repository.saveToken(new AuthToken(incomingLeadToken, incomingLeadId,
                        clock.instant().plusSeconds(tokenTtlSeconds)));
            }
        };
    }
}
