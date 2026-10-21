package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.UpdateBookingStatusRequestDto;
import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.security.AaptaPrincipal;
import com.ksiracare.backend.service.BookingService;
import com.ksiracare.backend.time.EpochTime;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The signed-in aapta's bookings. */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** Bookings starting in [startTime, endTime), epoch milliseconds. */
    @GetMapping
    public BookingsResponseDto getBookings(@AuthenticationPrincipal AaptaPrincipal me,
                                           @RequestParam Long startTime,
                                           @RequestParam Long endTime) {
        return bookingService.getBookings(me.aaptaId(), EpochTime.toRange(startTime, endTime));
    }

    /** Mark complete / client no-show, or undo with "PENDING". */
    @PatchMapping("/{bookingId}")
    public BookingItemDto updateStatus(@AuthenticationPrincipal AaptaPrincipal me,
                                       @PathVariable UUID bookingId,
                                       @Valid @RequestBody UpdateBookingStatusRequestDto request) {
        return bookingService.updateStatus(me.aaptaId(), bookingId, request.bookingStatus());
    }
}
