package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.AdminProperties;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.BookingNotCancellableException;
import com.ksiracare.backend.exception.BookingNotEditableException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminBookingPolicyTest {

    private final AdminBookingPolicy policy = new AdminBookingPolicy(new AdminProperties(Duration.ofHours(24)));

    /** Tomorrow (Mon 12 Oct) at 13:00 IST. */
    private final Booking tomorrowAt13 = Booking.builder()
            .startTime(utc("2026-10-12T13:00"))
            .endTime(utc("2026-10-12T14:00"))
            .build();

    @Test
    void opensExactly24HoursBeforeTheStart() {
        assertThat(policy.editableFrom(tomorrowAt13)).isEqualTo(utc("2026-10-11T13:00"));
        assertThat(policy.isEditable(tomorrowAt13, utc("2026-10-11T12:59"))).as("now 12:59 today").isFalse();
        assertThat(policy.isEditable(tomorrowAt13, utc("2026-10-11T13:00"))).as("now 13:00 today").isTrue();
    }

    @Test
    void closesWhenTheSessionStarts() {
        assertThat(policy.isEditable(tomorrowAt13, utc("2026-10-12T12:59"))).isTrue();
        assertThat(policy.isEditable(tomorrowAt13, utc("2026-10-12T13:00"))).isFalse();
        assertThatThrownBy(() -> policy.checkEditable(tomorrowAt13, utc("2026-10-12T15:00")))
                .isInstanceOf(BookingNotEditableException.class)
                .hasMessageContaining("24 hours");
    }

    @Test
    void cancelledBookingsAreNeverEditable() {
        Booking cancelled = Booking.builder()
                .startTime(utc("2026-10-12T13:00"))
                .endTime(utc("2026-10-12T14:00"))
                .status(BookingStatus.CANCELLED)
                .build();

        assertThat(policy.isEditable(cancelled, utc("2026-10-12T10:00"))).isFalse();
    }

    @Test
    void onlyUnmarkedBookingsCanBeCancelledWhateverTheTime() {
        Booking pending = Booking.builder().startTime(utc("2026-09-01T10:00")).endTime(utc("2026-09-01T11:00")).build();
        Booking completed = Booking.builder().startTime(utc("2026-10-12T13:00")).endTime(utc("2026-10-12T14:00"))
                .status(BookingStatus.COMPLETED).build();
        Booking cancelled = Booking.builder().startTime(utc("2026-10-12T13:00")).endTime(utc("2026-10-12T14:00"))
                .status(BookingStatus.CANCELLED).build();

        assertThat(policy.isCancellable(pending)).as("long past, never marked").isTrue();
        assertThatThrownBy(() -> policy.checkCancellable(completed))
                .isInstanceOf(BookingNotCancellableException.class)
                .hasMessageContaining("marked");
        assertThatThrownBy(() -> policy.checkCancellable(cancelled))
                .isInstanceOf(BookingNotCancellableException.class)
                .hasMessageContaining("already cancelled");
    }
}
