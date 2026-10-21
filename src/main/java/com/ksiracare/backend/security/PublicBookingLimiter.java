package com.ksiracare.backend.security;

import com.ksiracare.backend.config.PublicBookingProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Caps how many bookings one client address may make per window, so the public booking form
 * can't be used to flood the admin's Today page. Only bookings actually made count.
 */
@Component
public class PublicBookingLimiter {

    private final SlidingWindowCounter bookings;

    public PublicBookingLimiter(PublicBookingProperties properties, Clock clock) {
        this.bookings = new SlidingWindowCounter(properties.maxPerClient(), properties.rateWindow(), clock);
    }

    public boolean isBlocked(String clientAddress) {
        return bookings.isFull(clientAddress);
    }

    public void recordBooking(String clientAddress) {
        bookings.record(clientAddress);
    }
}
