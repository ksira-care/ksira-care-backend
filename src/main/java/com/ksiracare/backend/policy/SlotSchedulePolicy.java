package com.ksiracare.backend.policy;

import com.ksiracare.backend.time.UtcRange;

import java.time.LocalDateTime;
import java.util.List;

/** Which hours are bookable sessions, and when a therapist may still open or close them. */
public interface SlotSchedulePolicy {

    /** Every session start within the range, in order (stored UTC). */
    List<LocalDateTime> slotStartsWithin(UtcRange range);

    /** Whether a moment (stored UTC) is a session start, e.g. on the hour within opening hours. */
    boolean isSlotStart(LocalDateTime slotStartUtc);

    /** Whether the therapist may still change this hour: not yet started, and within the booking window. */
    boolean isOpenForChanges(LocalDateTime slotStartUtc, LocalDateTime nowUtc);
}
