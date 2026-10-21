package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.Language;

import java.util.Set;
import java.util.UUID;

/**
 * What the client entered when booking ("View form").
 *
 * @param submittedAt epoch milliseconds (UTC)
 */
public record BookingFormDto(
        UUID bookingId,
        String name,
        String email,
        String phone,
        String country,
        Set<Language> preferredLanguages,
        String reason,
        Long submittedAt
) {
}
