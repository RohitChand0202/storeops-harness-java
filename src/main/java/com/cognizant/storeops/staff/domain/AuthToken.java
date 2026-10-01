package com.cognizant.storeops.staff.domain;

import java.time.Instant;

/**
 * A bearer token.
 *
 * @param token the opaque token value
 * @param userId who it authenticates
 * @param expiresAt when it stops being valid
 */
public record AuthToken(String token, String userId, Instant expiresAt) {
}
