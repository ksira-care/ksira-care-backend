package com.ksiracare.backend.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.ksiracare.backend.support.TestClocks.calendarAt;
import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThat;

class PortalCalendarTest {

    @Test
    void todayFollowsIstEvenWhenUtcIsStillYesterday() {
        // 01:30 IST on 3 Oct is 20:00 UTC on 2 Oct.
        PortalCalendar calendar = calendarAt("2026-10-03T01:30");

        assertThat(calendar.today()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(calendar.dateOf(calendar.nowUtc())).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    void monthIsTheIstCalendarMonthAndEndExclusive() {
        PortalCalendar calendar = calendarAt("2026-10-15T12:00");

        UtcRange october = calendar.monthOf(calendar.today());

        assertThat(october.start()).isEqualTo(utc("2026-10-01T00:00"));
        assertThat(october.end()).isEqualTo(utc("2026-11-01T00:00"));
        assertThat(october.contains(utc("2026-10-31T23:59"))).isTrue();
        assertThat(october.contains(utc("2026-11-01T00:00"))).isFalse();
    }
}
