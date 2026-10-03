package com.ksiracare.backend.policy;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.BookingStatusChangeException;
import com.ksiracare.backend.support.TestClocks;
import com.ksiracare.backend.time.PortalCalendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingStatusPolicyTest {

    private final PortalCalendar calendar = TestClocks.calendarAt("2026-10-03T15:00");
    private final BookingStatusPolicy policy = new BookingStatusPolicy(calendar);
    private final LocalDateTime now = calendar.nowUtc();

    private static Booking booking(String istStart, BookingStatus status, String istMarkedAt) {
        return Booking.builder()
                .startTime(utc(istStart))
                .endTime(utc(istStart).plusHours(1))
                .status(status)
                .markedAt(istMarkedAt == null ? null : utc(istMarkedAt))
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"COMPLETED", "CLIENT_NO_SHOW"})
    void aStartedPendingSessionCanBeMarked(BookingStatus mark) {
        assertThatCode(() -> policy.checkTherapistChange(booking("2026-10-03T14:30", BookingStatus.PENDING, null), mark, now))
                .doesNotThrowAnyException();
    }

    @Test
    void aSessionCannotBeMarkedBeforeItStarts() {
        assertThatThrownBy(() -> policy.checkTherapistChange(
                booking("2026-10-03T17:00", BookingStatus.PENDING, null), BookingStatus.COMPLETED, now))
                .isInstanceOf(BookingStatusChangeException.class)
                .hasMessageContaining("started");
    }

    @Test
    void aSessionCannotBeMarkedTwice() {
        assertThatThrownBy(() -> policy.checkTherapistChange(
                booking("2026-10-03T10:00", BookingStatus.COMPLETED, "2026-10-03T11:10"), BookingStatus.CLIENT_NO_SHOW, now))
                .isInstanceOf(BookingStatusChangeException.class);
    }

    @Test
    void aMarkCanBeUndoneOnTheSameIstDay() {
        assertThatCode(() -> policy.checkTherapistChange(
                booking("2026-10-02T09:00", BookingStatus.COMPLETED, "2026-10-03T00:30"), BookingStatus.PENDING, now))
                .doesNotThrowAnyException();
    }

    @Test
    void aMarkFromAnEarlierDayCannotBeUndone() {
        assertThatThrownBy(() -> policy.checkTherapistChange(
                booking("2026-10-02T09:00", BookingStatus.COMPLETED, "2026-10-02T23:59"), BookingStatus.PENDING, now))
                .isInstanceOf(BookingStatusChangeException.class)
                .hasMessageContaining("day");
    }

    @Test
    void anAdminRecordedNoShowCannotBeUndone() {
        assertThatThrownBy(() -> policy.checkTherapistChange(
                booking("2026-10-03T10:00", BookingStatus.THERAPIST_NO_SHOW, "2026-10-03T12:00"), BookingStatus.PENDING, now))
                .isInstanceOf(BookingStatusChangeException.class);
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"THERAPIST_NO_SHOW", "CANCELLED"})
    void adminOnlyStatusesAreRefused(BookingStatus target) {
        assertThatThrownBy(() -> policy.checkTherapistChange(booking("2026-10-03T10:00", BookingStatus.PENDING, null), target, now))
                .isInstanceOf(BookingStatusChangeException.class)
                .hasMessageContaining("admin");
    }
}
