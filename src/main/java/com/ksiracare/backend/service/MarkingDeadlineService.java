package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Aaptas mark each session by the end of its day (portal time zone). One still unmarked after
 * that counts as the aapta's no-show — which means the client is refunded. Sessions with no
 * aapta are left alone: there was nobody to mark them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkingDeadlineService {

    static final String NOTE = "Not marked by the end of the session's day";

    private final BookingRepository bookingRepository;
    private final BookingHistory history;
    private final PortalCalendar calendar;

    /** Applies the deadline to every session from before today. Safe to run any number of times. */
    @Transactional
    public int recordMissedMarks() {
        LocalDateTime startOfToday = calendar.dayOf(calendar.today()).start();
        LocalDateTime now = calendar.nowUtc();
        List<Booking> missed = bookingRepository.findUnmarkedAllocatedStartingBefore(startOfToday);
        for (Booking booking : missed) {
            booking.recordAaptaNoShow(now);
            history.recordBySystem(booking, BookingEventType.AAPTA_NO_SHOW_RECORDED, now, NOTE);
        }
        if (!missed.isEmpty()) {
            log.info("Recorded {} unmarked session(s) as aapta no-shows", missed.size());
        }
        return missed.size();
    }
}
