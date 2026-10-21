package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.enums.Language;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * A client booking a session on the website.
 *
 * @param startTime the session start, epoch milliseconds — one of the hours from {@code GET /public/availability}
 * @param languages ISO 639-1 codes the client is comfortable in, e.g. ["hi", "en"]; at least one
 * @param notes     what they'd like from the session ("What they expected"), optional
 * @param consent   they accepted that this is a listening service, not therapy; must be true
 */
public record PublicBookingRequestDto(
        @NotBlank(message = "Name is required") @Size(min = 2, max = 80, message = "Name must be 2–80 characters") String name,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 255) String email,
        @NotNull(message = "startTime is required") Long startTime,
        @NotEmpty(message = "Choose at least one language") Set<@NotNull Language> languages,
        @Size(max = 500, message = "Keep notes under 500 characters") String notes,
        @NotNull(message = "Consent is required") @AssertTrue(message = "Consent is required") Boolean consent
) {
}
