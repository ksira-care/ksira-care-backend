package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.Language;

import java.util.Set;
import java.util.UUID;

/**
 * One aapta on the admin roster.
 *
 * @param email     their login
 * @param languages ISO 639-1 codes, e.g. ["en", "hi"]; the UI formats them ("EN, HI")
 */
public record AaptaSummaryDto(
        UUID aaptaId,
        String firstName,
        String middleName,
        String lastName,
        String email,
        Set<Language> languages
) {
}
