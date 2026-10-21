package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.PublicBookingRequestDto;
import com.ksiracare.backend.dto.response.BookableHoursDto;
import com.ksiracare.backend.dto.response.PublicBookingDto;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.service.PublicBookingService;
import com.ksiracare.backend.time.QueryRanges;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Booking from the website, no sign-in. Only exists while {@code ksira.public-booking.enabled}
 * is on; otherwise these paths answer 404.
 */
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@ConditionalOnBooleanProperty("ksira.public-booking.enabled")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;
    private final QueryRanges queryRanges;

    /**
     * Hours in [startTime, endTime) (epoch milliseconds) a client speaking any of {@code languages}
     * can book, e.g. {@code ?startTime=…&endTime=…&languages=hi,en}.
     */
    @GetMapping("/availability")
    public BookableHoursDto getAvailability(@RequestParam long startTime,
                                            @RequestParam long endTime,
                                            @RequestParam Set<Language> languages) {
        return publicBookingService.getBookableHours(queryRanges.of(startTime, endTime), languages);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public PublicBookingDto book(@Valid @RequestBody PublicBookingRequestDto request, HttpServletRequest http) {
        return publicBookingService.book(request, http.getRemoteAddr());
    }
}
