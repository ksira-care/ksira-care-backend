package com.ksiracare.backend.support;

import com.ksiracare.backend.config.AuthProperties;
import com.ksiracare.backend.security.Role;

import java.time.Duration;
import java.util.Map;

/** Sign-in settings for unit tests: 2-hour sessions, 5 failures per 15 minutes. */
public final class TestAuth {

    private TestAuth() {
    }

    public static AuthProperties properties(String base64Secret) {
        return new AuthProperties(Duration.ofHours(2), base64Secret,
                new AuthProperties.Cookie("Strict", true, Map.of(
                        Role.AAPTA, new AuthProperties.RoleCookie("ksira_session", "/api"),
                        Role.ADMIN, new AuthProperties.RoleCookie("ksira_admin_session", "/api/admin"))),
                new AuthProperties.LoginAttempts(5, Duration.ofMinutes(15)));
    }
}
