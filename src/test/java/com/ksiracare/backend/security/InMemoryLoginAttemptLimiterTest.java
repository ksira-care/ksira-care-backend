package com.ksiracare.backend.security;

import com.ksiracare.backend.config.AuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptLimiterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-03T10:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final LoginAttemptLimiter limiter = new InMemoryLoginAttemptLimiter(new AuthProperties(
            Duration.ofHours(2), "unused",
            new AuthProperties.Cookie("c", "/api", "Strict", true),
            new AuthProperties.LoginAttempts(5, Duration.ofMinutes(15))), clock);

    @Test
    void blocksAfterFiveFailuresRegardlessOfEmailCase() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("therapist@ksiracare.com");
        }
        assertThat(limiter.isBlocked("therapist@ksiracare.com")).isFalse();

        limiter.recordFailure("Therapist@KsiraCare.com ");

        assertThat(limiter.isBlocked("therapist@ksiracare.com")).isTrue();
        assertThat(limiter.isBlocked("someone-else@ksiracare.com")).isFalse();
    }

    @Test
    void unblocksOnceFailuresAgeOutOfTheWindow() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("therapist@ksiracare.com");
        }
        now.set(now.get().plus(Duration.ofMinutes(16)));

        assertThat(limiter.isBlocked("therapist@ksiracare.com")).isFalse();
    }

    @Test
    void aSuccessfulSignInClearsFailures() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("therapist@ksiracare.com");
        }
        limiter.recordSuccess("therapist@ksiracare.com");
        limiter.recordFailure("therapist@ksiracare.com");

        assertThat(limiter.isBlocked("therapist@ksiracare.com")).isFalse();
    }
}
