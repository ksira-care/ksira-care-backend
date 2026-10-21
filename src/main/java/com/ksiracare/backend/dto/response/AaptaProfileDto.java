package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.Language;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * The signed-in aapta's profile. Also used by the portal on every page load to
 * check the session and show the aapta's name.
 *
 * @param phone       E.164, e.g. "+919876543210"
 * @param dateOfBirth ISO date, e.g. "1991-03-14"
 * @param languages   ISO 639-1 codes, e.g. ["en", "hi"]
 */
public record AaptaProfileDto(
        UUID aaptaId,
        String email,
        String firstName,
        String middleName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        Set<Language> languages,
        String address
) {
}
