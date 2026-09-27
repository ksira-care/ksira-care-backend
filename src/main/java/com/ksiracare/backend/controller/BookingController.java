package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.UpdateBookingStatusRequestDto;
import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<BookingsResponseDto> getBookings(
            @RequestParam UUID therapistId,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam(required = false) BookingStatus status) {
        return ResponseEntity.ok(bookingService.getBookings(therapistId, startTime, endTime, status));
    }

    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<BookingItemDto> updateBookingStatus(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateBookingStatusRequestDto request) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(bookingId, request.getStatus()));
    }
}
