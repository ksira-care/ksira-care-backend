package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.BookingMapper;
import com.ksiracare.backend.policy.BookingStatusPolicy;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    /** Therapists don't see cancelled sessions. */
    private static final Set<BookingStatus> HIDDEN_FROM_THERAPISTS = Set.of(BookingStatus.CANCELLED);

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingStatusPolicy statusPolicy;
    private final PortalCalendar calendar;

    @Override
    @Transactional(readOnly = true)
    public BookingsResponseDto getBookings(UUID therapistId, UtcRange range) {
        return new BookingsResponseDto(bookingMapper.toItems(
                bookingRepository.findForTherapist(therapistId, range.start(), range.end(), HIDDEN_FROM_THERAPISTS)));
    }

    @Override
    @Transactional
    public BookingItemDto updateStatus(UUID therapistId, UUID bookingId, BookingStatus target) {
        // Someone else's booking is reported as "not found", so its existence isn't revealed.
        Booking booking = bookingRepository.findOwnedBy(bookingId, therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));

        LocalDateTime now = calendar.nowUtc();
        statusPolicy.checkTherapistChange(booking, target, now);

        booking.setStatus(target);
        booking.setMarkedAt(target == BookingStatus.PENDING ? null : now);
        return bookingMapper.toItem(bookingRepository.save(booking));
    }
}
