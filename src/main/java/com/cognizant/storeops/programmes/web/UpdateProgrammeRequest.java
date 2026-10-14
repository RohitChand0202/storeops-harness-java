package com.cognizant.storeops.programmes.web;

import jakarta.validation.constraints.NotNull;

import com.cognizant.storeops.programmes.domain.ProjectStatus;

/** The single supported programme state update. */
public record UpdateProgrammeRequest(@NotNull ProjectStatus status) {
}
