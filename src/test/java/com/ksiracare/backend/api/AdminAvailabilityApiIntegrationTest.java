package com.ksiracare.backend.api;

import com.jayway.jsonpath.JsonPath;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin availability grid. Uses a day far ahead so only this class's hours are on it (each
 * test uses its own hours); every active aapta, including the demo ones, still gets a row.
 */
class AdminAvailabilityApiIntegrationTest extends ApiTestSupport {

    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PortalCalendar calendar;

    private LocalDate day;
    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        day = calendar.today().plusDays(200);
        admin = adminSession();
    }

    @Test
    void everyAaptaGetsACellPerHourAsTheySetIt() throws Exception {
        Aapta anika = aapta("Anika", "Rao");
        slotRepository.save(new Slot(anika, at(10), SlotStatus.AAPTA_AVAILABLE));
        slotRepository.save(new Slot(anika, at(11), SlotStatus.AAPTA_UNAVAILABLE));
        Slot booked = slotRepository.save(new Slot(anika, at(12), SlotStatus.BOOKED));
        Booking booking = bookingRepository.save(Booking.builder()
                .aapta(anika).slot(booked)
                .customerName("Farah Ali").customerEmail("farah@example.com")
                .startTime(at(12)).endTime(at(13))
                .build());

        overview()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(day.toString()))
                .andExpect(jsonPath("$.hours", hasSize(15)))
                .andExpect(jsonPath("$.hours[0].time").value(EpochTime.toMillis(at(9))))
                .andExpect(cell(anika, 9, "status").value("AAPTA_UNAVAILABLE"))
                .andExpect(cell(anika, 10, "status").value("AAPTA_AVAILABLE"))
                .andExpect(cell(anika, 11, "status").value("AAPTA_UNAVAILABLE"))
                .andExpect(cell(anika, 12, "status").value("BOOKED"))
                .andExpect(cell(anika, 12, "booking.bookingId").value(booking.getId().toString()))
                .andExpect(cell(anika, 12, "booking.client.name").value("Farah Ali"))
                .andExpect(cell(anika, 10, "booking").value(contains(nullValue())));
    }

    @Test
    void theFooterCountsOpenAaptasPerHour() throws Exception {
        Aapta first = aapta("Open", "One");
        Aapta second = aapta("Open", "Two");
        slotRepository.save(new Slot(first, at(15), SlotStatus.AAPTA_AVAILABLE));
        slotRepository.save(new Slot(second, at(15), SlotStatus.AAPTA_AVAILABLE));
        slotRepository.save(new Slot(second, at(16), SlotStatus.BOOKED));

        overview()
                .andExpect(hour(15).value(2))
                .andExpect(hour(16).value(0))
                .andExpect(hour(9).value(0));
    }

    @Test
    void deactivatedAaptasAppearOnlyOnDaysTheyHadBookings() throws Exception {
        Aapta left = aapta("Left", "Withbooking");
        slotRepository.save(new Slot(left, at(14), SlotStatus.BOOKED));
        left.setActive(false);
        aaptaRepository.save(left);
        Aapta gone = aapta("Gone", "Quiet");
        gone.setActive(false);
        aaptaRepository.save(gone);

        overview()
                .andExpect(jsonPath("$.aaptas[?(@.aaptaId == '%s')]".formatted(left.getId()), hasSize(1)))
                .andExpect(jsonPath("$.aaptas[?(@.aaptaId == '%s')]".formatted(gone.getId()), empty()));
    }

    @Test
    void rowsAreSortedByName() throws Exception {
        String body = overview().andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(body, "$.aaptas[*].firstName");

        assertThat(names).isNotEmpty().isSorted();
    }

    @Test
    void onlyAdminsCanSeeIt() throws Exception {
        mvc.perform(get("/api/admin/availability").contextPath("/api").param("date", day.toString()).cookie(session()))
                .andExpect(status().isForbidden());
    }

    // ── helpers ──────────────────────────────────────────────

    private Aapta aapta(String first, String last) {
        String email = "%s.%s.%s@example.com".formatted(first, last, UUID.randomUUID()).toLowerCase();
        return aaptaRepository.save(new Aapta(first, last, email, passwordEncoder.encode(DEMO_PASSWORD)));
    }

    private LocalDateTime at(int istHour) {
        return calendar.toUtc(day, LocalTime.of(istHour, 0));
    }

    private ResultActions overview() throws Exception {
        return mvc.perform(get("/api/admin/availability").contextPath("/api").param("date", day.toString()).cookie(admin));
    }

    /** The given aapta's cell at an IST hour (hours start at 09:00, so index = hour − 9). */
    private static JsonPathResultMatchers cell(Aapta aapta, int istHour, String field) {
        return jsonPath("$.aaptas[?(@.aaptaId == '%s')].slots[%d].%s".formatted(aapta.getId(), istHour - 9, field));
    }

    private static JsonPathResultMatchers hour(int istHour) {
        return jsonPath("$.hours[%d].openAaptas".formatted(istHour - 9));
    }
}
