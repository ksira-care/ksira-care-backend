package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.time.UtcRange;

import java.util.UUID;

public interface BookingService {

    /** The aapta's bookings starting in the range, cancelled ones excluded. */
    BookingsResponseDto getBookings(UUID aaptaId, UtcRange range);

    /** Changes the status of one of the aapta's own bookings, following {@code BookingStatusPolicy}. */
    BookingItemDto updateStatus(UUID aaptaId, UUID bookingId, BookingStatus target);
}
