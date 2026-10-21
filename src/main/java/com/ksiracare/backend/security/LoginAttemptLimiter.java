package com.ksiracare.backend.security;

/**
 * Slows down password guessing by refusing sign-ins after repeated failures. Keys identify
 * the account being guessed at, e.g. role plus email; they are compared case-insensitively.
 */
public interface LoginAttemptLimiter {

    /** Whether sign-ins for this key are currently refused. */
    boolean isBlocked(String key);

    void recordFailure(String key);

    void recordSuccess(String key);
}
