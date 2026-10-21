package com.ksiracare.backend.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Converts between API timestamps (epoch milliseconds) and stored UTC {@link LocalDateTime}s. */
public final class EpochTime {

    private EpochTime() {
    }

    public static Long toMillis(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static LocalDateTime toUtc(Long epochMillis) {
        return epochMillis == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }
}
