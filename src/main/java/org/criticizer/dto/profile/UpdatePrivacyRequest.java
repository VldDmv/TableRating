package org.criticizer.dto.profile;

import jakarta.validation.constraints.NotNull;

/** Request DTO for updating profile privacy */
public record UpdatePrivacyRequest(
        @NotNull(message = "Privacy setting is required") Boolean isPublic) {}
