package com.ksiracare.backend.dto.response;

import java.util.List;

public record BookingsResponseDto(List<BookingItemDto> bookings) {
}
