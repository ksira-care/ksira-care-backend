package com.ksiracare.backend.config;

import com.ksiracare.backend.security.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;

/**
 * Sign-in settings ({@code ksira.auth.*}).
 *
 * @param sessionTtl    how long a session lasts; there is no refresh
 * @param jwtSecret     base64-encoded HMAC key, at least 256 bits
 * @param cookie        how session cookies are issued
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

    /**
     * @param roles each role's own cookie, so an aapta and an admin session never overwrite
     *              each other and each is only sent to its own part of the API
     */
    public record Cookie(@NotBlank String sameSite, boolean secure, @NotNull Map<Role, @Valid RoleCookie> roles) {

        public Cookie {
            if (roles != null && !roles.keySet().containsAll(EnumSet.allOf(Role.class))) {
                throw new IllegalArgumentException("ksira.auth.cookie.roles must configure every role " + EnumSet.allOf(Role.class));
            }
        }

        public RoleCookie of(Role role) {
            return roles.get(role);
        }
    }

    public record RoleCookie(@NotBlank String name, @NotBlank String path) {
    }

    public record LoginAttempts(@Min(1) int maxFailures, @NotNull Duration window) {
    }
}
