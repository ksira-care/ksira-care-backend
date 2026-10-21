package com.ksiracare.backend.entity;

import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.PortalCalendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Created/updated timestamps are stored in UTC like every other time, whatever the host's zone. */
@SpringBootTest
class StoredTimestampsTest {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private PortalCalendar calendar;

    @Test
    void creationTimestampsAreUtc() {
        LocalDateTime start = calendar.nowUtc().plusDays(1);
        Booking booking = bookingRepository.save(Booking.builder()
                .customerName("Client").customerEmail("client@example.com")
                .startTime(start).endTime(start.plusHours(1))
                .build());

        Duration drift = Duration.between(calendar.nowUtc(), booking.getCreatedAt()).abs();
        assertThat(drift).isLessThan(Duration.ofMinutes(1));
    }
}
