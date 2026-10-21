package com.ksiracare.backend.policy;

import com.ksiracare.backend.enums.AaptaField;
import com.ksiracare.backend.exception.FieldNotEditableException;
import com.ksiracare.backend.security.Role;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Who may change which part of an aapta's profile:
 * <ul>
 *   <li>The aapta: their name and personal details (phone, date of birth, address) and languages.</li>
 *   <li>An admin: the login email and languages — never personal details.</li>
 * </ul>
 */
@Component
public class AaptaProfileEditPolicy {

    private static final Set<AaptaField> AAPTA_EDITS = EnumSet.of(
            AaptaField.FIRST_NAME, AaptaField.MIDDLE_NAME, AaptaField.LAST_NAME,
            AaptaField.PHONE, AaptaField.DATE_OF_BIRTH, AaptaField.ADDRESS, AaptaField.LANGUAGES);
    private static final Set<AaptaField> ADMIN_EDITS = EnumSet.of(AaptaField.EMAIL, AaptaField.LANGUAGES);

    public Set<AaptaField> editableBy(Role role) {
        return switch (role) {
            case AAPTA -> AAPTA_EDITS;
            case ADMIN -> ADMIN_EDITS;
        };
    }

    /** Throws {@link FieldNotEditableException} naming any field {@code role} may not change. */
    public void check(Role role, Set<AaptaField> fields) {
        Set<AaptaField> refused = EnumSet.noneOf(AaptaField.class);
        refused.addAll(fields);
        refused.removeAll(editableBy(role));
        if (!refused.isEmpty()) {
            throw new FieldNotEditableException("You can't change: " + refused.stream()
                    .map(field -> field.name().toLowerCase().replace('_', ' '))
                    .collect(Collectors.joining(", ")) + ".");
        }
    }
}
