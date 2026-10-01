package com.cognizant.storeops.programmes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound body for opening a programme.
 *
 * @param name programme name
 * @param storeId store it runs in
 * @param ownerId staff member who owns it
 */
public record CreateProgrammeRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank String storeId,
        @NotBlank String ownerId) {
}
