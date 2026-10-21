package com.ksiracare.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An admin calling a session off.
 *
 * @param reason  why, kept in the booking's history
 * @param version the booking's {@code version} as last loaded
 */
public record AdminCancelBookingRequestDto(
        @NotBlank(message = "A reason is required") @Size(max = 500, message = "Keep the reason under 500 characters") String reason,
        @NotNull(message = "version is required") Long version
) {
}
