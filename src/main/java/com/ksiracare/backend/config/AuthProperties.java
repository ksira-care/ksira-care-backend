package com.ksiracare.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Sign-in settings ({@code ksira.auth.*}).
 *
 * @param sessionTtl    how long a session lasts; there is no refresh
 * @param jwtSecret     base64-encoded HMAC key, at least 256 bits
 * @param cookie        how the session cookie is issued
 * @param loginAttempts limits on failed sign-ins
 */
@Validated
@ConfigurationProperties("ksira.auth")
public record AuthProperties(
        @NotNull Duration sessionTtl,
        @NotBlank(message = "ksira.auth.jwt-secret must be set (KSIRA_AUTH_JWT_SECRET)") String jwtSecret,
        @Valid @NotNull Cookie cookie,
        @Valid @NotNull LoginAttempts loginAttempts
) {

    public record Cookie(@NotBlank String name, @NotBlank String path, @NotBlank String sameSite, boolean secure) {
    }

    public record LoginAttempts(@Min(1) int maxFailures, @NotNull Duration window) {
    }
}
