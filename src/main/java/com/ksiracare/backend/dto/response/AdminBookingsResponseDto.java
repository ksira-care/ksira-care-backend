package com.ksiracare.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

/** One portal-time-zone day of bookings, in start order. */
public record AdminBookingsResponseDto(LocalDate date, List<AdminBookingDto> bookings) {
}
