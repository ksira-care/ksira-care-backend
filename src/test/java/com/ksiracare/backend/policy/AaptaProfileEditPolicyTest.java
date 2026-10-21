package com.ksiracare.backend.policy;

import com.ksiracare.backend.enums.AaptaField;
import com.ksiracare.backend.exception.FieldNotEditableException;
import com.ksiracare.backend.security.Role;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AaptaProfileEditPolicyTest {

    private final AaptaProfileEditPolicy policy = new AaptaProfileEditPolicy();

    @Test
    void aaptasEditTheirNameAndPersonalDetailsButNotTheirLogin() {
        assertThatCode(() -> policy.check(Role.AAPTA, EnumSet.complementOf(EnumSet.of(AaptaField.EMAIL))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.check(Role.AAPTA, EnumSet.of(AaptaField.PHONE, AaptaField.EMAIL)))
                .isInstanceOf(FieldNotEditableException.class)
                .hasMessage("You can't change: email.");
    }

    @Test
    void adminsEditTheLoginAndLanguagesButNeverPersonalDetails() {
        assertThatCode(() -> policy.check(Role.ADMIN, EnumSet.of(AaptaField.EMAIL, AaptaField.LANGUAGES)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.check(Role.ADMIN, EnumSet.of(AaptaField.FIRST_NAME, AaptaField.DATE_OF_BIRTH)))
                .isInstanceOf(FieldNotEditableException.class)
                .hasMessage("You can't change: first name, date of birth.");
    }
}
