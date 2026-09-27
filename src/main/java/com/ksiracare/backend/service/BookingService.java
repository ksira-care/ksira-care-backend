package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.enums.BookingStatus;

import java.util.UUID;

public interface BookingService {

    BookingsResponseDto getBookings(UUID therapistId, Long startTime, Long endTime, BookingStatus statusFilter);

    BookingItemDto updateBookingStatus(UUID bookingId, BookingStatus newStatus);
}
