package com.ksiracare.backend.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Converts between API timestamps (epoch milliseconds) and stored UTC {@link LocalDateTime}s. */
public final class EpochTime {

    /** Epoch seconds stay below this until the year 2286, so it tells seconds from milliseconds. */
    private static final long SECONDS_THRESHOLD = 10_000_000_000L;

    private EpochTime() {
    }

    public static Long toMillis(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /** Accepts epoch milliseconds (preferred) or seconds. */
    public static LocalDateTime toUtc(Long epoch) {
        if (epoch == null) {
            return null;
        }
        Instant instant = epoch < SECONDS_THRESHOLD ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static UtcRange toRange(Long startEpoch, Long endEpoch) {
        return new UtcRange(toUtc(startEpoch), toUtc(endEpoch));
    }
}
