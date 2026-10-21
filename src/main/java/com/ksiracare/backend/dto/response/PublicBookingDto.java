package com.ksiracare.backend.dto.response;

import java.util.UUID;

/** A booking made on the website. Times are epoch milliseconds. */
public record PublicBookingDto(UUID bookingId, Long startTime, Long endTime) {
}
