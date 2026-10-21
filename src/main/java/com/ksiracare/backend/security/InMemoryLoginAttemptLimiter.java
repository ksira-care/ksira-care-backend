package com.ksiracare.backend.security;

import com.ksiracare.backend.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Blocks an email after N failed sign-ins within a sliding window (see {@link SlidingWindowCounter}). */
@Component
public class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

    private final SlidingWindowCounter failures;

    public InMemoryLoginAttemptLimiter(AuthProperties properties, Clock clock) {
        this.failures = new SlidingWindowCounter(
                properties.loginAttempts().maxFailures(), properties.loginAttempts().window(), clock);
    }

    @Override
    public boolean isBlocked(String key) {
        return failures.isFull(key);
    }

    @Override
    public void recordFailure(String key) {
        failures.record(key);
    }

    @Override
    public void recordSuccess(String key) {
        failures.clear(key);
    }
}
