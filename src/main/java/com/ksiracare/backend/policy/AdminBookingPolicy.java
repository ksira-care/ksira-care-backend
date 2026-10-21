package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.AdminProperties;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.BookingNotCancellableException;
import com.ksiracare.backend.exception.BookingNotEditableException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * What an admin may do to a booking, and when:
 * <ul>
 *   <li>Allocate or change its aapta, set the BCC flag: from {@code bookingActionWindow} (24 h)
 *       before it starts until it starts. Before that, and after, it is view-only.</li>
 *   <li>Cancel it: any time, as long as the aapta hasn't marked it yet (still PENDING).</li>
 * </ul>
 * Outcomes are never set here — they come from the aapta's portal.
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

    /** Only sessions that haven't been marked (or cancelled) can be called off; there is no time limit. */
    public boolean isCancellable(Booking booking) {
        return booking.getStatus() == BookingStatus.PENDING;
    }

    /** Throws {@link BookingNotCancellableException} if the booking can't be cancelled. */
    public void checkCancellable(Booking booking) {
        if (!isCancellable(booking)) {
            throw new BookingNotCancellableException(booking.getStatus() == BookingStatus.CANCELLED
                    ? "This booking is already cancelled."
                    : "This session has already been marked by the aapta, so it can't be cancelled.");
        }
    }

    /** Throws {@link BookingNotEditableException} if the booking can't be changed right now. */
    public void checkEditable(Booking booking, LocalDateTime nowUtc) {
        if (!isEditable(booking, nowUtc)) {
            throw new BookingNotEditableException(
                    "This booking can only be changed in the " + window.toHours() + " hours before it starts.");
        }
    }
}
