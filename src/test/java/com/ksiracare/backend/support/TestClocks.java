package com.ksiracare.backend.support;

import com.ksiracare.backend.config.PortalProperties;
import com.ksiracare.backend.time.PortalCalendar;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Fixed clocks and an IST portal calendar for time-based unit tests. */
public final class TestClocks {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    public static final PortalProperties PORTAL = new PortalProperties(IST, 60, 9, 23);

    private TestClocks() {
    }

    /** A clock stopped at the given IST wall-clock time. */
    public static Clock atIst(String isoLocalDateTime) {
        return Clock.fixed(LocalDateTime.parse(isoLocalDateTime).atZone(IST).toInstant(), ZoneOffset.UTC);
    }

    public static PortalCalendar calendarAt(String isoLocalDateTime) {
        return new PortalCalendar(atIst(isoLocalDateTime), PORTAL);
    }

    /** An IST wall-clock time as the stored UTC value. */
    public static LocalDateTime utc(String isoLocalDateTime) {
        return LocalDateTime.parse(isoLocalDateTime).atZone(IST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
