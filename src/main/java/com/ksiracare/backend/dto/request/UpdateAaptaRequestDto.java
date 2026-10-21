package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.enums.AaptaField;
import com.ksiracare.backend.enums.Language;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * An edit to an aapta's profile — the same body whether the aapta edits themselves
 * ({@code PATCH /aaptas/me}) or an admin edits them ({@code PATCH /admin/aaptas/{id}}); which
 * fields each may send is decided by {@code AaptaProfileEditPolicy}.
 * <p>
 * Only the fields sent change: a missing or null field is left as it is. Send {@code ""} to
 * clear an optional text field (middle name, phone, address).
 *
 * @param phone       E.164, e.g. "+919876543210"
 * @param dateOfBirth ISO date, e.g. "1991-03-14"
 * @param languages   ISO 639-1 codes, e.g. ["en", "hi"]; at least one
 * @param email       their login; changing it signs them out everywhere
 */
public record UpdateAaptaRequestDto(
        @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*", message = "First name can't be blank") String firstName,
        @Size(max = 100) String middleName,
        @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*", message = "Last name can't be blank") String lastName,
        @Pattern(regexp = "^$|^\\+?[1-9]\\d{1,14}$", message = "Must be a valid E.164 phone number") String phone,
        @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
        @Size(max = 255) String address,
        @Size(min = 1, message = "Choose at least one language") Set<@NotNull Language> languages,
        @Email(message = "Invalid email format") @Size(min = 3, max = 255) String email
) {

    /** The fields this edit would change. */
    public Set<AaptaField> fieldsSent() {
        Set<AaptaField> sent = EnumSet.noneOf(AaptaField.class);
        if (firstName != null) sent.add(AaptaField.FIRST_NAME);
        if (middleName != null) sent.add(AaptaField.MIDDLE_NAME);
        if (lastName != null) sent.add(AaptaField.LAST_NAME);
        if (phone != null) sent.add(AaptaField.PHONE);
        if (dateOfBirth != null) sent.add(AaptaField.DATE_OF_BIRTH);
        if (address != null) sent.add(AaptaField.ADDRESS);
        if (languages != null) sent.add(AaptaField.LANGUAGES);
        if (email != null) sent.add(AaptaField.EMAIL);
        return sent;
    }
}
