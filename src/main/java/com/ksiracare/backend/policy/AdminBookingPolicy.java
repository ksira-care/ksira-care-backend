package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.AdminProperties;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.BookingNotEditableException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * When an admin may act on a booking (allocate or change its aapta, set the BCC flag): from
 * {@code bookingActionWindow} (24 h) before it starts until it starts. Before that, and after,
 * it is view-only. Outcomes are never set here — they come from the aapta's portal.
 */
@Component
public class AdminBookingPolicy {

    private final Duration window;

    public AdminBookingPolicy(AdminProperties properties) {
        this.window = properties.bookingActionWindow();
    }

    /** The first moment the booking can be changed (stored UTC). */
    public LocalDateTime editableFrom(Booking booking) {
        return booking.getStartTime().minus(window);
    }

    public boolean isEditable(Booking booking, LocalDateTime nowUtc) {
        return booking.getStatus() != BookingStatus.CANCELLED
                && !nowUtc.isBefore(editableFrom(booking))
                && nowUtc.isBefore(booking.getStartTime());
    }

    /** Throws {@link BookingNotEditableException} if the booking can't be changed right now. */
    public void checkEditable(Booking booking, LocalDateTime nowUtc) {
        if (!isEditable(booking, nowUtc)) {
            throw new BookingNotEditableException(
                    "This booking can only be changed in the " + window.toHours() + " hours before it starts.");
        }
    }
}
