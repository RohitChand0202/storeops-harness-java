package com.cognizant.storeops.staff.service;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.cognizant.storeops.shared.domain.StoreOpsUser;
import com.cognizant.storeops.shared.error.NotFoundException;
import com.cognizant.storeops.shared.error.UnauthorizedException;
import com.cognizant.storeops.staff.domain.AuthToken;
import com.cognizant.storeops.staff.domain.User;
import com.cognizant.storeops.staff.repository.UserRepository;

/** All staff business logic. */
@Service
public class StaffServiceImpl implements StaffService {

    private final UserRepository userRepository;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param userRepository staff store
     * @param clock injected clock, so token expiry is testable
     */
    public StaffServiceImpl(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NOT_FOUND", "User " + id + " does not exist"));
    }

    @Override
    public StoreOpsUser resolveToken(String token) {
        AuthToken found = userRepository.findToken(token)
                .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "Token is not recognised"));
        if (found.expiresAt().isBefore(clock.instant())) {
            throw new UnauthorizedException("UNAUTHORIZED", "Token has expired");
        }
        User user = findById(found.userId());
        return new StoreOpsUser(user.id(), user.storeId(), user.regionId(), user.role().name());
    }
}
