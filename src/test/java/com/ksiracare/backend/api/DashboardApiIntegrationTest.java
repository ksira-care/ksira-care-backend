package com.ksiracare.backend.api;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.PortalCalendar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The aapta dashboard's numbers: completed sessions this month and in the last 30 days. */
class DashboardApiIntegrationTest extends ApiTestSupport {

    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PortalCalendar calendar;

    @Test
    void countsOnlyRecentCompletedSessions() throws Exception {
        String email = "dash.%s@example.com".formatted(UUID.randomUUID());
        Aapta aapta = aaptaRepository.save(new Aapta("Dia", "Shah", email, passwordEncoder.encode(DEMO_PASSWORD)));
        LocalDate today = calendar.today();
        List<LocalDate> completedOn = List.of(today, today.minusDays(30), today.minusDays(31));
        completedOn.forEach(day -> booking(aapta, day, BookingStatus.COMPLETED));
        booking(aapta, today, BookingStatus.CLIENT_NO_SHOW);

        long thisMonth = completedOn.stream().filter(day -> YearMonth.from(day).equals(YearMonth.from(today))).count();
        mvc.perform(get("/api/aaptas/me/dashboard-summary").contextPath("/api").cookie(session(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLast30Days").value(2))
                .andExpect(jsonPath("$.completedThisMonth").value(thisMonth));
    }

    private void booking(Aapta aapta, LocalDate day, BookingStatus status) {
        var start = calendar.toUtc(day, LocalTime.of(0, 30));
        bookingRepository.save(Booking.builder()
                .aapta(aapta)
                .customerName("Tom H").customerEmail("tom@example.com")
                .startTime(start).endTime(start.plusHours(1))
                .status(status)
                .build());
    }
}
