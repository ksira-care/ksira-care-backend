package com.ksiracare.backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Save from the admin console: "Add" (first aapta) and "Update" (change aapta and/or BCC) alike.
 *
 * @param aaptaId      who should hold the session; must have opened that hour, unless unchanged
 * @param bccEmailSent whether the client and aapta have been emailed the Meet link
 * @param version      the booking's {@code version} as last loaded
 */
public record AdminBookingUpdateRequestDto(
        @NotNull(message = "aaptaId is required") UUID aaptaId,
        @NotNull(message = "bccEmailSent is required") Boolean bccEmailSent,
        @NotNull(message = "version is required") Long version
) {
}
