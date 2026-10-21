package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/** Changing your own password: the current one proves it's you. */
public record ChangePasswordRequestDto(
        @NotBlank(message = "Current password is required") String currentPassword,
        @ValidPassword String newPassword
) {
}
