package com.ksiracare.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A password an account may be given: 8 to 72 characters (BCrypt ignores anything past 72 bytes,
 * so longer ones would be silently truncated). The one place the password rule is defined.
 */
@NotNull
@Size(min = 8, max = 72)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Documented
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
public @interface ValidPassword {

    String message() default "Password must be 8 to 72 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
