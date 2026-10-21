package com.ksiracare.backend.support;

import com.ksiracare.backend.config.AuthProperties;

import java.time.Duration;

/** Sign-in settings for unit tests: 2-hour sessions, 5 failures per 15 minutes. */
public final class TestAuth {

    private TestAuth() {
    }

    public static AuthProperties properties(String base64Secret) {
        return new AuthProperties(Duration.ofHours(2), base64Secret,
                new AuthProperties.Cookie("ksira_session", "/api", "Strict", true),
                new AuthProperties.LoginAttempts(5, Duration.ofMinutes(15)));
    }
}
