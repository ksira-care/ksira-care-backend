package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.SlotStatus;

import java.util.UUID;

/**
 * One aapta at one hour, as they set it in their portal.
 *
 * @param time    session start, epoch milliseconds (UTC)
 * @param status  AAPTA_AVAILABLE (open), BOOKED or AAPTA_UNAVAILABLE (not working, or never opened)
 * @param booking the session in this hour when BOOKED, otherwise null
 */
public record AvailabilityCellDto(Long time, SlotStatus status, BookedSessionDto booking) {

    /** Who the hour is booked for (shown on hover). */
    public record BookedSessionDto(UUID bookingId, ClientDto client) {
    }
}
