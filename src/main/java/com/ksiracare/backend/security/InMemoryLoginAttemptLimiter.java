package com.ksiracare.backend.security;

import com.ksiracare.backend.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks an email after N failed sign-ins within a sliding window. Kept in memory, which is
 * enough for a single instance; with several instances behind a load balancer, swap in a
 * shared store (e.g. Redis) behind the same interface.
 */
@Component
public class InMemoryLoginAttemptLimiter implements LoginAttemptLimiter {

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public InMemoryLoginAttemptLimiter(AuthProperties properties, Clock clock) {
        this.maxFailures = properties.loginAttempts().maxFailures();
        this.window = properties.loginAttempts().window();
        this.clock = clock;
    }

    @Override
    public boolean isBlocked(String email) {
        Deque<Instant> recent = failures.get(key(email));
        if (recent == null) {
            return false;
        }
        synchronized (recent) {
            dropExpired(recent);
            return recent.size() >= maxFailures;
        }
    }

    @Override
    public void recordFailure(String email) {
        Deque<Instant> recent = failures.computeIfAbsent(key(email), k -> new ArrayDeque<>());
        synchronized (recent) {
            dropExpired(recent);
            recent.addLast(clock.instant());
        }
    }

    @Override
    public void recordSuccess(String email) {
        failures.remove(key(email));
    }

    private void dropExpired(Deque<Instant> recent) {
        Instant cutoff = clock.instant().minus(window);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.pollFirst();
        }
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
