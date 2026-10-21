package com.ksiracare.backend.policy;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.BookingStatusChangeException;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Which booking status changes an aapta may make:
 * <ul>
 *   <li>Mark a pending session COMPLETED or CLIENT_NO_SHOW — once it has started, and only on its
 *       own portal-zone day: after that it is recorded as an aapta no-show (see MarkingDeadlineService).</li>
 *   <li>Undo their own mark (back to PENDING) — only on the portal-zone day they made it.</li>
 *   <li>Anything else (AAPTA_NO_SHOW, CANCELLED, …) is for admins only.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class BookingStatusPolicy {

    private static final Set<BookingStatus> AAPTA_MARKS = Set.of(BookingStatus.COMPLETED, BookingStatus.CLIENT_NO_SHOW);

    private final PortalCalendar calendar;

    /** Throws {@link BookingStatusChangeException} if the change isn't allowed right now. */
    public void checkAaptaChange(Booking booking, BookingStatus target, LocalDateTime nowUtc) {
        if (AAPTA_MARKS.contains(target)) {
            checkMark(booking, nowUtc);
        } else if (target == BookingStatus.PENDING) {
            checkUndo(booking, nowUtc);
        } else {
            throw new BookingStatusChangeException("Only an admin can set a booking to " + target + ".");
        }
    }

    private void checkMark(Booking booking, LocalDateTime nowUtc) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingStatusChangeException("This session has already been marked.");
        }
        if (nowUtc.isBefore(booking.getStartTime())) {
            throw new BookingStatusChangeException("A session can only be marked once it has started.");
        }
        if (!calendar.dateOf(booking.getStartTime()).equals(calendar.dateOf(nowUtc))) {
            throw new BookingStatusChangeException("A session can only be marked on the day it takes place.");
        }
    }

    private void checkUndo(Booking booking, LocalDateTime nowUtc) {
        boolean markedByAapta = AAPTA_MARKS.contains(booking.getStatus()) && booking.getMarkedAt() != null;
        if (!markedByAapta) {
            throw new BookingStatusChangeException("There's no mark to undo on this session.");
        }
        if (!calendar.dateOf(booking.getMarkedAt()).equals(calendar.dateOf(nowUtc))) {
            throw new BookingStatusChangeException("A mark can only be undone on the day it was made.");
        }
    }
}
