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
 * Blocks a key (role + email) after N failed sign-ins within a sliding window. Kept in memory, which is
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
    public boolean isBlocked(String key) {
        Deque<Instant> recent = failures.get(normalise(key));
        if (recent == null) {
            return false;
        }
        synchronized (recent) {
            dropExpired(recent);
            return recent.size() >= maxFailures;
        }
    }

    @Override
    public void recordFailure(String key) {
        Deque<Instant> recent = failures.computeIfAbsent(normalise(key), k -> new ArrayDeque<>());
        synchronized (recent) {
            dropExpired(recent);
            recent.addLast(clock.instant());
        }
    }

    @Override
    public void recordSuccess(String key) {
        failures.remove(normalise(key));
    }

    private void dropExpired(Deque<Instant> recent) {
        Instant cutoff = clock.instant().minus(window);
        while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
            recent.pollFirst();
        }
    }

    private static String normalise(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }
}
