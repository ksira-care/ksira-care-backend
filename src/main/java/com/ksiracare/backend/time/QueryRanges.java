package com.ksiracare.backend.time;

import com.ksiracare.backend.config.PortalProperties;
import com.ksiracare.backend.exception.InvalidRangeException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Turns a client's {@code startTime}/{@code endTime} query parameters (epoch milliseconds) into a
 * {@link UtcRange}, rejecting empty, reversed or oversized ranges — an unbounded range would make
 * the server build an arbitrarily large response.
 */
@Component
public class QueryRanges {

    private final Duration maxLength;

    public QueryRanges(PortalProperties properties) {
        this.maxLength = properties.maxQueryRange();
    }

    public UtcRange of(long startEpochMillis, long endEpochMillis) {
        LocalDateTime start = EpochTime.toUtc(startEpochMillis);
        LocalDateTime end = EpochTime.toUtc(endEpochMillis);
        if (!start.isBefore(end)) {
            throw new InvalidRangeException("startTime must be before endTime.");
        }
        if (Duration.between(start, end).compareTo(maxLength) > 0) {
            throw new InvalidRangeException("The range can span at most " + maxLength.toDays() + " days.");
        }
        return new UtcRange(start, end);
    }
}
