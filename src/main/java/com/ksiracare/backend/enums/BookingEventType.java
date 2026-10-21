package com.ksiracare.backend.enums;

/** Changes recorded in a booking's history. */
public enum BookingEventType {
    /** First aapta given to a booking that had none. */
    AAPTA_ALLOCATED,
    /** Booking moved from one aapta to another. */
    AAPTA_REALLOCATED,
    /** Booking taken away from its aapta (they were removed); it waits for a new allocation. */
    AAPTA_UNALLOCATED,
    BCC_EMAIL_MARKED_SENT,
    BCC_EMAIL_MARKED_NOT_SENT,
    /** An admin called the session off; the event's note says why. */
    BOOKING_CANCELLED
}
