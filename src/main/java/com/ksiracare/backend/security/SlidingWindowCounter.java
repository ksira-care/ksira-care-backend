package com.ksiracare.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts events per key within a sliding window, in memory — enough for a single instance; with
 * several instances, swap the callers onto a shared store (e.g. Redis). Keys are compared
 * case-insensitively.
 */
public final class SlidingWindowCounter {

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    public SlidingWindowCounter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /** Whether {@code key} has had {@code limit} events within the window. */
    public boolean isFull(String key) {
        Deque<Instant> recent = events.get(normalise(key));
        if (recent == null) {
            return false;
        }
        synchronized (recent) {
            dropExpired(recent);
            return recent.size() >= limit;
        }
    }

    public void record(String key) {
        Deque<Instant> recent = events.computeIfAbsent(normalise(key), k -> new ArrayDeque<>());
        synchronized (recent) {
            dropExpired(recent);
            recent.addLast(clock.instant());
        }
    }

    public void clear(String key) {
        events.remove(normalise(key));
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
