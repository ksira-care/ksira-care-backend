package com.ksiracare.backend.security;

/** Slows down password guessing by refusing sign-ins after repeated failures. */
public interface LoginAttemptLimiter {

    /** Whether this account is currently blocked from signing in. */
    boolean isBlocked(String email);

    void recordFailure(String email);

    void recordSuccess(String email);
}
