package com.ksiracare.backend.time;

import com.ksiracare.backend.config.PortalProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The portal's notion of "today", "this month" and wall-clock time. Data is stored in UTC,
 * but therapists live by the portal time zone (IST), so every calendar rule goes through here.
 */
@Component
public class PortalCalendar {

    private final Clock clock;
    private final ZoneId zone;

    public PortalCalendar(Clock clock, PortalProperties properties) {
        this.clock = clock;
        this.zone = properties.timeZone();
    }

    public LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /** The portal-zone date on which a stored UTC moment falls. */
    public LocalDate dateOf(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate();
    }

    /** The portal-zone wall-clock time of a stored UTC moment. */
    public LocalTime timeOf(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalTime();
    }

    /** A portal-zone date and time, as a stored UTC moment. */
    public LocalDateTime toUtc(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public UtcRange dayOf(LocalDate date) {
        return new UtcRange(toUtc(date, LocalTime.MIDNIGHT), toUtc(date.plusDays(1), LocalTime.MIDNIGHT));
    }

    public UtcRange monthOf(LocalDate date) {
        LocalDate first = date.withDayOfMonth(1);
        return new UtcRange(toUtc(first, LocalTime.MIDNIGHT), toUtc(first.plusMonths(1), LocalTime.MIDNIGHT));
    }
}
