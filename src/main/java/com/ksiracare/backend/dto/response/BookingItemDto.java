package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.Language;

import java.util.Set;
import java.util.UUID;

/**
 * One session as the therapist sees it. All times are epoch milliseconds (UTC).
 * Customer contact details, country and fees are deliberately not exposed.
 *
 * @param rescheduledFrom the original start, if an admin moved the session
 * @param markedAt        when the therapist marked it, or null while pending
 */
public record BookingItemDto(
        UUID bookingId,
        Long startTime,
        Long endTime,
        String customerName,
        String bookingReason,
        BookingStatus bookingStatus,
        Set<Language> customerPreferredLanguages,
        Long assignedAt,
        Long rescheduledFrom,
        String rescheduleNote,
        Long markedAt
) {
}
