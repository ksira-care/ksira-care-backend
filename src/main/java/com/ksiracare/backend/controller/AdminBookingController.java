package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.AdminBookingUpdateRequestDto;
import com.ksiracare.backend.dto.response.AaptaOptionsResponseDto;
import com.ksiracare.backend.dto.response.AdminBookingDto;
import com.ksiracare.backend.dto.response.AdminBookingsResponseDto;
import com.ksiracare.backend.dto.response.BookingFormDto;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.service.AdminBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Bookings in the admin console: the Today / Tomorrow / any-date views and allocation. */
@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    /** Every booking on one day, e.g. {@code ?date=2026-10-11} (portal time zone). */
    @GetMapping
    public AdminBookingsResponseDto getDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new AdminBookingsResponseDto(date, adminBookingService.getDay(date));
    }

    /** What the client entered when booking. */
    @GetMapping("/{bookingId}/form")
    public BookingFormDto getForm(@PathVariable UUID bookingId) {
        return adminBookingService.getForm(bookingId);
    }

    /** Who the booking can be given to. */
    @GetMapping("/{bookingId}/aapta-options")
    public AaptaOptionsResponseDto getAaptaOptions(@PathVariable UUID bookingId) {
        return new AaptaOptionsResponseDto(adminBookingService.getAaptaOptions(bookingId));
    }

    /** Add / Update: allocate or change the aapta and set the BCC flag. 409 outside the 24-hour window. */
    @PatchMapping("/{bookingId}")
    public AdminBookingDto update(@AuthenticationPrincipal AdminPrincipal me,
                                  @PathVariable UUID bookingId,
                                  @Valid @RequestBody AdminBookingUpdateRequestDto request) {
        return adminBookingService.update(me, bookingId, request);
    }
}
