package com.ksiracare.backend.time;

import java.time.LocalDateTime;

/**
 * A span of UTC time; {@code start} inclusive, {@code end} exclusive — so back-to-back
 * ranges (days, months) never count the same instant twice.
 */
public record UtcRange(LocalDateTime start, LocalDateTime end) {

    public UtcRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Range start and end are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Range start must be before its end");
        }
    }

    public boolean contains(LocalDateTime instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }
}
