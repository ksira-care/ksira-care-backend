package com.ksiracare.backend.api;

import com.jayway.jsonpath.JsonPath;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Admin;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.BookingEvent;
import com.ksiracare.backend.entity.HistoryExport;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.Actor;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.AdminRepository;
import com.ksiracare.backend.repository.BookingEventRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.HistoryExportRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.PortalCalendar;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** History: this month and last, the CSV of last month, and deleting months once exported. */
class AdminHistoryApiIntegrationTest extends ApiTestSupport {

    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingEventRepository eventRepository;
    @Autowired private HistoryExportRepository exportRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PortalCalendar calendar;

    private Cookie admin;
    private YearMonth thisMonth;
    private YearMonth lastMonth;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminSession();
        thisMonth = calendar.currentMonth();
        lastMonth = thisMonth.minusMonths(1);
    }

    // ── view ─────────────────────────────────────────────────

    @Test
    void showsEveryBookingOfTheMonthWhateverItsStatus() throws Exception {
        Aapta ravi = aapta("Ravi", "Shah");
        Booking refunded = booking(ravi, at(lastMonth, 3, 17), BookingStatus.AAPTA_NO_SHOW, "Just wants someone to listen.");
        Booking cancelled = booking(null, at(lastMonth, 4, 10), BookingStatus.CANCELLED, null);

        history(lastMonth)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(lastMonth.toString()))
                .andExpect(jsonPath("$.months", contains(thisMonth.toString(), lastMonth.toString())))
                .andExpect(row(refunded, "bookingStatus").value("AAPTA_NO_SHOW"))
                .andExpect(row(refunded, "refunded").value(true))
                .andExpect(row(refunded, "aapta.firstName").value("Ravi"))
                .andExpect(row(refunded, "notes").value("Just wants someone to listen."))
                .andExpect(row(cancelled, "bookingStatus").value("CANCELLED"))
                .andExpect(row(cancelled, "refunded").value(false));

        String body = history(lastMonth).andReturn().getResponse().getContentAsString();
        int total = JsonPath.read(body, "$.summary.total");
        List<Object> sessions = JsonPath.read(body, "$.sessions");
        List<Long> starts = JsonPath.read(body, "$.sessions[*].startTime");
        assertThat(total).isEqualTo(sessions.size());
        assertThat(starts).as("newest first").isSortedAccordingTo((a, b) -> Long.compare(b, a));
        int refundedCount = JsonPath.read(body, "$.summary.refunded");
        assertThat(refundedCount).isPositive();
    }

    @Test
    void theCurrentMonthIsTheDefaultAndOnlyTwoMonthsAreShown() throws Exception {
        mvc.perform(get("/api/admin/history").contextPath("/api").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(thisMonth.toString()));
        history(thisMonth.minusMonths(2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HISTORY_MONTH_NOT_AVAILABLE"));
    }

    // ── export ───────────────────────────────────────────────

    @Test
    void exportIsAlwaysLastMonthWithEveryFieldAndFormulasDefused() throws Exception {
        Aapta ravi = aapta("Ravi", "Shah");
        Booking booking = booking(ravi, at(lastMonth, 5, 18), BookingStatus.COMPLETED, "=HYPERLINK(\"http://evil\")");
        Booking thisMonths = booking(ravi, at(thisMonth, 1, 9), BookingStatus.PENDING, null);

        MvcResult result = export()
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"ksira-sessions-%s.csv\"".formatted(lastMonth)))
                .andReturn();
        String csv = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿\"Booking ID\",\"Date\",\"Start\"");
        String line = csv.lines().filter(l -> l.contains(booking.getId().toString())).findFirst().orElseThrow();
        assertThat(line)
                .contains("\"18:00\"", "\"Ravi Shah\"", "\"" + ravi.getEmail() + "\"", "\"Completed\"")
                .contains("\"'=HYPERLINK(\"\"http://evil\"\")\"")
                .contains("\"-\"");
        assertThat(csv).doesNotContain(thisMonths.getId().toString());
    }

    @Test
    void exportingNeverHappensThroughAGet() throws Exception {
        mvc.perform(get("/api/admin/history/export").contextPath("/api").cookie(admin))
                .andExpect(status().isNotFound());
    }

    // ── deleting old months ──────────────────────────────────

    @Test
    void aMonthNobodyExportedIsKeptAndFlagged() throws Exception {
        YearMonth forgotten = thisMonth.minusMonths(5);
        Booking old = booking(aapta("Old", "Timer"), at(forgotten, 10, 11), BookingStatus.COMPLETED, null);

        export()
                .andExpect(status().isOk())
                .andExpect(header().string("X-Not-Exported-Months", containsString(forgotten.toString())));

        assertThat(bookingRepository.findById(old.getId())).isPresent();
        history(thisMonth).andExpect(jsonPath("$.olderMonthsNotExported", hasItem(forgotten.toString())));
    }

    @Test
    void aMonthExportedBeforeIsDeletedWithEverythingHangingOffIt() throws Exception {
        YearMonth exportedEarlier = thisMonth.minusMonths(3);
        Aapta aapta = aapta("Gone", "Soon");
        LocalDateTime start = at(exportedEarlier, 12, 15);
        Slot slot = slotRepository.save(new Slot(aapta, start, SlotStatus.BOOKED));
        Slot openHour = slotRepository.save(new Slot(aapta, start.plusHours(2), SlotStatus.AAPTA_AVAILABLE));
        Booking old = bookingRepository.save(Booking.builder()
                .aapta(aapta).slot(slot)
                .customerName("Tom H").customerEmail("tom@example.com")
                .startTime(start).endTime(start.plusHours(1))
                .status(BookingStatus.COMPLETED)
                .build());
        recordEvent(old);
        Booking keptLastMonth = booking(aapta, at(lastMonth, 2, 12), BookingStatus.COMPLETED, null);
        Admin someone = adminRepository.findByEmailIgnoreCase("admin@ksiracare.com").orElseThrow();
        exportRepository.save(new HistoryExport(exportedEarlier, someone.getId(), calendar.nowUtc()));

        export()
                .andExpect(status().isOk())
                .andExpect(header().string("X-Deleted-Months", containsString(exportedEarlier.toString())));

        assertThat(bookingRepository.findById(old.getId())).isEmpty();
        assertThat(slotRepository.findById(slot.getId())).isEmpty();
        assertThat(slotRepository.findById(openHour.getId())).isEmpty();
        assertThat(eventRepository.findForBookings(List.of(old.getId()), EnumSet.allOf(BookingEventType.class))).isEmpty();
        assertThat(bookingRepository.findById(keptLastMonth.getId())).as("inside the window").isPresent();
        history(thisMonth).andExpect(jsonPath("$.olderMonthsNotExported", not(hasItem(exportedEarlier.toString()))));
    }

    @Test
    void onlyAdminsCanSeeOrExportHistory() throws Exception {
        mvc.perform(get("/api/admin/history").contextPath("/api").cookie(session())).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/history/export").contextPath("/api").cookie(session())).andExpect(status().isForbidden());
    }

    // ── helpers ──────────────────────────────────────────────

    private Aapta aapta(String first, String last) {
        String email = "%s.%s.%s@example.com".formatted(first, last, UUID.randomUUID()).toLowerCase();
        return aaptaRepository.save(new Aapta(first, last, email, passwordEncoder.encode(DEMO_PASSWORD)));
    }

    private LocalDateTime at(YearMonth month, int day, int istHour) {
        return calendar.toUtc(month.atDay(day), LocalTime.of(istHour, 0));
    }

    private Booking booking(Aapta aapta, LocalDateTime start, BookingStatus status, String notes) {
        return bookingRepository.save(Booking.builder()
                .aapta(aapta)
                .customerName("Tom H").customerEmail("tom@example.com")
                .bookingReason(notes)
                .startTime(start).endTime(start.plusHours(1))
                .status(status)
                .build());
    }

    private void recordEvent(Booking booking) {
        eventRepository.save(BookingEvent.builder()
                .booking(booking)
                .type(BookingEventType.AAPTA_ALLOCATED)
                .actor(Actor.SYSTEM)
                .aapta(booking.getAapta())
                .occurredAt(calendar.nowUtc())
                .build());
    }

    private ResultActions history(YearMonth month) throws Exception {
        return mvc.perform(get("/api/admin/history").contextPath("/api").param("month", month.toString()).cookie(admin));
    }

    private ResultActions export() throws Exception {
        return mvc.perform(post("/api/admin/history/export").contextPath("/api").cookie(admin));
    }

    private static JsonPathResultMatchers row(Booking booking, String field) {
        return jsonPath("$.sessions[?(@.bookingId == '%s')].%s".formatted(booking.getId(), field));
    }
}
