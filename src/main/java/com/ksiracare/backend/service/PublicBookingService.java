package com.ksiracare.backend.service;

import com.ksiracare.backend.config.PortalProperties;
import com.ksiracare.backend.config.PublicBookingProperties;
import com.ksiracare.backend.dto.request.PublicBookingRequestDto;
import com.ksiracare.backend.dto.response.BookableHoursDto;
import com.ksiracare.backend.dto.response.PublicBookingDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.exception.ApiException;
import com.ksiracare.backend.exception.ErrorCode;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.exception.TimeNotAvailableException;
import com.ksiracare.backend.policy.SlotSchedulePolicy;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.LanguageRepository;
import com.ksiracare.backend.security.PublicBookingLimiter;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Clients booking a session on the website. A session can be booked from {@code leadTime} ahead
 * (book at 14:00, the first offer is 16:00) to the end of the booking window, at an hour where an
 * aapta who speaks one of the client's languages is still free. The booking arrives unallocated:
 * an admin gives it to an aapta on the Today page.
 * <p>
 * Payment (Razorpay) is not taken yet, so a booking is confirmed as soon as it is made. When it
 * is, {@link #book} becomes the step that holds the hour while the client pays.
 */
@Service
@RequiredArgsConstructor
public class PublicBookingService {

    private final BookingRepository bookingRepository;
    private final LanguageRepository languageRepository;
    private final HourCapacity capacity;
    private final SlotSchedulePolicy schedule;
    private final PortalCalendar calendar;
    private final PortalProperties portal;
    private final PublicBookingProperties properties;
    private final PublicBookingLimiter limiter;

    /** The hours in {@code requested} that a client speaking {@code languages} can book right now. */
    @Transactional(readOnly = true)
    public BookableHoursDto getBookableHours(UtcRange requested, Set<Language> languages) {
        UtcRange window = bookingWindow();
        if (!requested.start().isBefore(window.end()) || !requested.end().isAfter(window.start())) {
            return new BookableHoursDto(List.of());
        }
        UtcRange range = new UtcRange(max(requested.start(), window.start()), min(requested.end(), window.end()));
        return new BookableHoursDto(capacity.hoursWithRoomFor(range, languages).stream()
                .map(start -> new BookableHoursDto.Hour(EpochTime.toMillis(start), EpochTime.toMillis(end(start))))
                .toList());
    }

    /**
     * Books the session if the hour is still bookable for these languages; otherwise 409
     * TIME_NOT_AVAILABLE (someone just took it) or 422 INVALID_SLOT_TIME (it never was).
     *
     * @param clientAddress where the request came from, for the per-client limit
     */
    @Transactional
    public PublicBookingDto book(PublicBookingRequestDto request, String clientAddress) {
        if (limiter.isBlocked(clientAddress)) {
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    "You've made several bookings in a short time. Please try again later.");
        }
        LocalDateTime start = EpochTime.toUtc(request.startTime());
        checkBookable(start);
        if (!capacity.hasRoomFor(start, request.languages())) {
            throw new TimeNotAvailableException();
        }

        Booking booking = bookingRepository.save(Booking.builder()
                .customerName(request.name().trim())
                .customerEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .customerPreferredLanguages(languageRepository.findSet(request.languages()))
                .bookingReason(StringUtils.hasText(request.notes()) ? request.notes().trim() : null)
                .consentGivenAt(calendar.nowUtc())
                .startTime(start)
                .endTime(end(start))
                .build());
        limiter.recordBooking(clientAddress);
        return new PublicBookingDto(booking.getId(), EpochTime.toMillis(start), EpochTime.toMillis(booking.getEndTime()));
    }

    private void checkBookable(LocalDateTime start) {
        UtcRange window = bookingWindow();
        if (!schedule.isSlotStart(start)) {
            throw new InvalidSlotTimeException("Sessions start on the hour, between the first and last session of the day.");
        }
        if (start.isBefore(window.start())) {
            throw new InvalidSlotTimeException("Sessions can be booked from " + hours(properties.leadTime()) + " ahead.");
        }
        if (!window.contains(start)) {
            throw new InvalidSlotTimeException("That date is too far ahead to book yet.");
        }
    }

    /** From {@code leadTime} from now to the end of the booking window. */
    private UtcRange bookingWindow() {
        return new UtcRange(calendar.nowUtc().plus(properties.leadTime()), schedule.windowEnd());
    }

    private LocalDateTime end(LocalDateTime start) {
        return start.plus(portal.sessionLength());
    }

    private static String hours(Duration duration) {
        long hours = duration.toHours();
        return hours == 1 ? "1 hour" : hours + " hours";
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
