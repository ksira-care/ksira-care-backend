package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.PortalCalendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MarkingDeadlineServiceTest {

    @Autowired private MarkingDeadlineService deadline;
    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PortalCalendar calendar;

    @Test
    void sessionsLeftUnmarkedPastTheirDayBecomeAaptaNoShows() {
        Aapta aapta = aaptaRepository.save(new Aapta("Late", "Marker", "late.%s@example.com".formatted(UUID.randomUUID()), "hash"));
        LocalDateTime startOfToday = calendar.dayOf(calendar.today()).start();

        Booking yesterday = booking(aapta, startOfToday.minusHours(3), BookingStatus.PENDING);
        Booking today = booking(aapta, startOfToday, BookingStatus.PENDING);
        Booking unallocated = booking(null, startOfToday.minusHours(3), BookingStatus.PENDING);
        Booking marked = booking(aapta, startOfToday.minusHours(5), BookingStatus.COMPLETED);

        deadline.recordMissedMarks();

        assertThat(statusOf(yesterday)).isEqualTo(BookingStatus.AAPTA_NO_SHOW);
        assertThat(bookingRepository.findById(yesterday.getId()).orElseThrow().getMarkedAt()).isNotNull();
        assertThat(statusOf(today)).as("its day isn't over").isEqualTo(BookingStatus.PENDING);
        assertThat(statusOf(unallocated)).as("nobody to mark it").isEqualTo(BookingStatus.PENDING);
        assertThat(statusOf(marked)).isEqualTo(BookingStatus.COMPLETED);

        assertThat(deadline.recordMissedMarks()).as("idempotent").isZero();
    }

    private Booking booking(Aapta aapta, LocalDateTime start, BookingStatus status) {
        return bookingRepository.save(Booking.builder()
                .aapta(aapta)
                .customerName("Client").customerEmail("client@example.com")
                .startTime(start).endTime(start.plusHours(1))
                .status(status)
                .build());
    }

    private BookingStatus statusOf(Booking booking) {
        return bookingRepository.findById(booking.getId()).orElseThrow().getStatus();
    }
}
