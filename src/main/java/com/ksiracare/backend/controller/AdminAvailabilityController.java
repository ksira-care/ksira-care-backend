package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.AvailabilityOverviewDto;
import com.ksiracare.backend.service.AdminAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** The admin console's availability grid. View only, any past or future date. */
@RestController
@RequestMapping("/admin/availability")
@RequiredArgsConstructor
public class AdminAvailabilityController {

    private final AdminAvailabilityService adminAvailabilityService;

    /** Every aapta's hours on one day, e.g. {@code ?date=2026-10-11} (portal time zone). */
    @GetMapping
    public AvailabilityOverviewDto getDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return adminAvailabilityService.getDay(date);
    }
}
