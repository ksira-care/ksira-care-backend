package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.PortalProperties;
import com.ksiracare.backend.support.TestClocks;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThat;

class FixedIntervalSlotSchedulePolicyTest {

    private final PortalCalendar calendar = TestClocks.calendarAt("2026-10-03T13:30");
    private final FixedIntervalSlotSchedulePolicy policy = new FixedIntervalSlotSchedulePolicy(calendar, TestClocks.PORTAL);

    @Test
    void offersFifteenHoursPerIstDayFrom9To23() {
        List<LocalDateTime> starts = policy.slotStartsWithin(new UtcRange(utc("2026-10-05T00:00"), utc("2026-10-06T00:00")));

        assertThat(starts).hasSize(15);
        assertThat(starts.getFirst()).isEqualTo(utc("2026-10-05T09:00"));
        assertThat(starts.getLast()).isEqualTo(utc("2026-10-05T23:00"));
    }

    @Test
    void recognisesSessionStartsInIst() {
        assertThat(policy.isSlotStart(utc("2026-10-05T09:00"))).isTrue();
        assertThat(policy.isSlotStart(utc("2026-10-05T23:00"))).isTrue();
        assertThat(policy.isSlotStart(utc("2026-10-05T08:00"))).isFalse();
        assertThat(policy.isSlotStart(utc("2026-10-05T09:30"))).isFalse();
    }

    @Test
    void onlyFutureHoursWithinSixtyDaysCanBeChanged() {
        LocalDateTime now = calendar.nowUtc();

        assertThat(policy.isOpenForChanges(utc("2026-10-03T13:00"), now)).as("already started").isFalse();
        assertThat(policy.isOpenForChanges(utc("2026-10-03T14:00"), now)).as("later today").isTrue();
        assertThat(policy.isOpenForChanges(utc("2026-12-02T23:00"), now)).as("day 60").isTrue();
        assertThat(policy.isOpenForChanges(utc("2026-12-03T09:00"), now)).as("day 61").isFalse();
    }

    @Test
    void followsTheConfiguredSessionLength() {
        PortalProperties halfHourly = new PortalProperties(TestClocks.IST, 60, 9, 11, Duration.ofMinutes(30), Duration.ofDays(92));
        FixedIntervalSlotSchedulePolicy policy = new FixedIntervalSlotSchedulePolicy(calendar, halfHourly);

        assertThat(policy.slotStartsWithin(new UtcRange(utc("2026-10-05T00:00"), utc("2026-10-06T00:00"))))
                .containsExactly(utc("2026-10-05T09:00"), utc("2026-10-05T09:30"), utc("2026-10-05T10:00"),
                        utc("2026-10-05T10:30"), utc("2026-10-05T11:00"));
        assertThat(policy.isSlotStart(utc("2026-10-05T10:30"))).isTrue();
        assertThat(policy.isSlotStart(utc("2026-10-05T10:15"))).isFalse();
        assertThat(policy.isSlotStart(utc("2026-10-05T11:30"))).as("after the last start").isFalse();
    }
}
