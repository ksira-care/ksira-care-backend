package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.Language;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * An aapta's profile, in one format everywhere: their own portal ({@code GET /aaptas/me}, also
 * the portal's "am I signed in?" check) and the admin console's roster and edit form.
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
