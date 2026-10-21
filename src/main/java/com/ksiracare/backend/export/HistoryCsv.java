package com.ksiracare.backend.export;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.service.BookingHistory;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The History export: one row per booking with every field we hold, times in the portal time zone. */
@Component
@RequiredArgsConstructor
public class HistoryCsv {

    private static final List<String> HEADER = List.of(
            "Booking ID", "Date", "Start", "End", "Client name", "Client email", "What they expected", "Consent",
            "Aapta", "Aapta email", "Previous aapta", "Outcome", "Refunded", "Marked at", "BCC email sent",
            "Assigned at", "Cancel reason", "Booked at");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final PortalCalendar calendar;

    public byte[] write(List<Booking> bookings, Map<UUID, BookingHistory.Summary> histories) {
        CsvWriter csv = new CsvWriter().row(HEADER);
        bookings.forEach(booking -> csv.row(row(booking, histories.get(booking.getId()))));
        return csv.toBytes();
    }

    private List<String> row(Booking booking, BookingHistory.Summary history) {
        List<String> cells = new ArrayList<>();
        cells.add(booking.getId().toString());
        cells.add(calendar.dateOf(booking.getStartTime()).toString());
        cells.add(time(booking.getStartTime()));
        cells.add(time(booking.getEndTime()));
        cells.add(booking.getCustomerName());
        cells.add(booking.getCustomerEmail());
        cells.add(booking.getBookingReason());
        cells.add(yesNo(booking.getConsentGivenAt() != null));
        cells.add(name(booking.getAapta()));
        cells.add(booking.getAapta() == null ? null : booking.getAapta().getEmail());
        cells.add(name(history.previousAapta()));
        cells.add(outcome(booking.getStatus()));
        cells.add(yesNo(booking.getStatus() == BookingStatus.AAPTA_NO_SHOW));
        cells.add(timestamp(booking.getMarkedAt()));
        cells.add(yesNo(booking.isBccEmailSent()));
        cells.add(timestamp(booking.getAssignedAt()));
        cells.add(history.cancelReason());
        cells.add(timestamp(booking.getCreatedAt()));
        return cells;
    }

    private static String outcome(BookingStatus status) {
        return switch (status) {
            case PENDING -> "Pending";
            case COMPLETED -> "Completed";
            case CLIENT_NO_SHOW -> "Client no-show";
            case AAPTA_NO_SHOW -> "Aapta no-show";
            case CANCELLED -> "Cancelled";
        };
    }

    private static String name(Aapta aapta) {
        return aapta == null ? null : aapta.getFirstName() + " " + aapta.getLastName();
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private String time(LocalDateTime utc) {
        return utc == null ? null : calendar.timeOf(utc).format(TIME);
    }

    private String timestamp(LocalDateTime utc) {
        return utc == null ? null : calendar.dateOf(utc) + " " + time(utc);
    }
}
