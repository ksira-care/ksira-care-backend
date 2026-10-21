package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.ClientDto;
import com.ksiracare.backend.dto.response.HistoryDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.mapper.AdminBookingMapper;
import com.ksiracare.backend.policy.HistoryWindow;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** The admin console's History: every booking of a recent month, with totals. */
@Service
@RequiredArgsConstructor
public class AdminHistoryService {

    private final BookingRepository bookingRepository;
    private final HistoryWindow window;
    private final HistoryRetention retention;
    private final AdminBookingMapper mapper;
    private final PortalCalendar calendar;

    @Transactional(readOnly = true)
    public HistoryDto getMonth(YearMonth month) {
        window.checkShown(month);
        List<Booking> bookings = bookingsIn(month);
        return new HistoryDto(month, window.months(), summarise(bookings), bookings.stream().map(this::toRow).toList(),
                retention.notExportedMonths());
    }

    /** Every booking starting in {@code month} (portal time zone), newest first, with its aapta loaded. */
    List<Booking> bookingsIn(YearMonth month) {
        UtcRange range = calendar.monthOf(month);
        return bookingRepository.findStartingBetween(range.start(), range.end()).stream()
                .sorted(Comparator.comparing(Booking::getStartTime).reversed())
                .toList();
    }

    /** Whether the client gets their money back: the aapta didn't show. */
    static boolean isRefunded(Booking booking) {
        return booking.getStatus() == BookingStatus.AAPTA_NO_SHOW;
    }

    private static HistoryDto.Summary summarise(List<Booking> bookings) {
        return new HistoryDto.Summary(
                bookings.size(),
                count(bookings, BookingStatus.COMPLETED),
                count(bookings, BookingStatus.CLIENT_NO_SHOW),
                count(bookings, BookingStatus.AAPTA_NO_SHOW),
                count(bookings, BookingStatus.CANCELLED),
                count(bookings, BookingStatus.PENDING));
    }

    private static int count(List<Booking> bookings, BookingStatus status) {
        return (int) bookings.stream().filter(booking -> booking.getStatus() == status).count();
    }

    private HistoryDto.Row toRow(Booking booking) {
        return new HistoryDto.Row(
                booking.getId(),
                EpochTime.toMillis(booking.getStartTime()),
                EpochTime.toMillis(booking.getEndTime()),
                new ClientDto(booking.getCustomerName(), booking.getCustomerEmail()),
                booking.getAapta() == null ? null : mapper.toRef(booking.getAapta()),
                booking.getStatus(),
                isRefunded(booking),
                booking.getBookingReason());
    }
}
