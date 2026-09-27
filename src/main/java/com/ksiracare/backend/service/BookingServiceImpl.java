package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.BookingMapper;
import com.ksiracare.backend.mapper.TherapistMapper;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.TherapistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final TherapistRepository therapistRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final TherapistMapper therapistMapper;

    @Override
    @Transactional(readOnly = true)
    public BookingsResponseDto getBookings(UUID therapistId, Long startTime, Long endTime, BookingStatus statusFilter) {
        if (therapistId == null) {
            throw new IllegalArgumentException("therapistId is required");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }

        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist Not Found!"));

        LocalDateTime startDateTime = bookingMapper.toLocalDateTime(startTime);
        LocalDateTime endDateTime = bookingMapper.toLocalDateTime(endTime);
        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("startTime must not be after endTime");
        }

        // Fetch bookings within range
        List<Booking> rawBookings = bookingRepository.findByTherapistIdAndStartTimeBetween(therapistId, startDateTime, endDateTime);

        // Apply optional status filter if present
        if (statusFilter != null) {
            rawBookings = rawBookings.stream()
                    .filter(b -> b.getStatus() == statusFilter)
            .collect(Collectors.toList());
        }

        List<BookingItemDto> bookingDtos = bookingMapper.bookingsToItemDtos(rawBookings);

        // Calculate dashboard statistics
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        // 1. Sessions this month
        LocalDateTime startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
        long sessionsThisMonth = bookingRepository.countByTherapistIdAndStartTimeBetween(therapistId, startOfMonth, endOfMonth);

        // 2. Today's sessions
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
        long todaysSessions = bookingRepository.countByTherapistIdAndStartTimeBetween(therapistId, startOfToday, endOfToday);

        // 3. Completed sessions overall
        long completedSessions = bookingRepository.countByTherapistIdAndStatus(therapistId, BookingStatus.COMPLETED);

        String therapistFullName = therapistMapper.buildFullName(therapist);

        return BookingsResponseDto.builder()
                .therapistName(therapistFullName)
                .sessionsThisMonth(sessionsThisMonth)
                .todaysSessions(todaysSessions)
                .completedSessions(completedSessions)
                .bookings(bookingDtos)
                .build();
    }

    @Override
    @Transactional
    public BookingItemDto updateBookingStatus(UUID bookingId, BookingStatus newStatus) {
        if (bookingId == null) {
            throw new IllegalArgumentException("bookingId is required");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("status is required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found!"));

        // Enforcement: Mark complete is only allowed once session start time has arrived/passed
        if (newStatus == BookingStatus.COMPLETED) {
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            if (nowUtc.isBefore(booking.getStartTime())) {
                throw new IllegalArgumentException("Session cannot be marked complete before its start time: " + booking.getStartTime());
            }
        }

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);

        return bookingMapper.bookingToItemDto(saved);
    }
}
