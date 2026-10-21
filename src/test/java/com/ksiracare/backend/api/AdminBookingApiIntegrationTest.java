package com.ksiracare.backend.api;

import com.jayway.jsonpath.JsonPath;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.BookingStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The admin console's day view and allocation, against real data. Each test creates its own
 * aaptas and bookings a few hours ahead, so it doesn't depend on the demo data or the clock.
 */
class AdminBookingApiIntegrationTest extends ApiTestSupport {

    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PortalCalendar calendar;

    private LocalDateTime soon;
    private Aapta anika;
    private Aapta ravi;
    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        soon = calendar.nowUtc().truncatedTo(ChronoUnit.HOURS).plusHours(3);
        anika = openAt(aapta("Anika", "Rao"), soon);
        ravi = openAt(aapta("Ravi", "Shah"), soon);
        admin = adminSession();
    }

    @Test
    void theDayViewListsBookingsWithEditabilityWorkedOutByTheServer() throws Exception {
        Booking unallocated = booking(soon, "Farah Ali");
        Booking later = booking(soon.plusHours(27), "Kabir Nair");

        dayOf(soon)
                .andExpect(status().isOk())
                .andExpect(row(unallocated, "client.name").value("Farah Ali"))
                .andExpect(row(unallocated, "client.email").value("farah-ali@example.com"))
                .andExpect(row(unallocated, "aapta").value(contains(nullValue())))
                .andExpect(row(unallocated, "bccEmailSent").value(false))
                .andExpect(row(unallocated, "bookingStatus").value("PENDING"))
                .andExpect(row(unallocated, "editable").value(true))
                .andExpect(row(unallocated, "editableFrom").value(EpochTime.toMillis(soon.minusHours(24))));
        dayOf(soon.plusHours(27))
                .andExpect(row(later, "editable").value(false));
    }

    @Test
    void cancelledBookingsAreListedButNeverEditable() throws Exception {
        Booking cancelled = booking(soon, "Omar Farouk");
        cancelled.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(cancelled);

        dayOf(soon)
                .andExpect(row(cancelled, "bookingStatus").value("CANCELLED"))
                .andExpect(row(cancelled, "editable").value(false));
        save(cancelled.getId(), anika.getId(), false, version(cancelled.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_EDITABLE"));
    }

    @Test
    void optionsAreTheAaptasWhoOpenedThatHour() throws Exception {
        Booking booking = booking(soon, "Farah Ali");
        Aapta closed = aapta("Closed", "Hours");
        slotRepository.save(new Slot(closed, soon, SlotStatus.AAPTA_UNAVAILABLE));
        Aapta inactive = openAt(aapta("Gone", "Away"), soon);
        inactive.setActive(false);
        aaptaRepository.save(inactive);

        mvc.perform(get("/api/admin/bookings/{id}/aapta-options", booking.getId()).contextPath("/api").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aaptas[*].aaptaId").value(hasItem(anika.getId().toString())))
                .andExpect(jsonPath("$.aaptas[*].aaptaId").value(hasItem(ravi.getId().toString())))
                .andExpect(jsonPath("$.aaptas[*].aaptaId").value(not(hasItem(closed.getId().toString()))))
                .andExpect(jsonPath("$.aaptas[*].aaptaId").value(not(hasItem(inactive.getId().toString()))));
    }

    @Test
    void addThenReassignMovesTheHourKeepsBccAndRecordsWhoHadIt() throws Exception {
        Booking booking = booking(soon, "Farah Ali");

        // Add: Anika, BCC sent.
        save(booking.getId(), anika.getId(), true, version(booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aapta.firstName").value("Anika"))
                .andExpect(jsonPath("$.bccEmailSent").value(true))
                .andExpect(jsonPath("$.previousAapta").doesNotExist());
        assertThat(slotOf(anika).getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertBookingVisibleTo(anika, booking, true);

        // Update: move to Ravi; BCC untouched, Anika's hour opens again.
        save(booking.getId(), ravi.getId(), true, version(booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aapta.firstName").value("Ravi"))
                .andExpect(jsonPath("$.previousAapta.firstName").value("Anika"))
                .andExpect(jsonPath("$.bccEmailSent").value(true))
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"));
        assertThat(slotOf(ravi).getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(slotOf(anika).getStatus()).isEqualTo(SlotStatus.AAPTA_AVAILABLE);
        assertBookingVisibleTo(anika, booking, false);
        assertBookingVisibleTo(ravi, booking, true);

        dayOf(soon).andExpect(row(booking, "previousAapta.lastName").value("Rao"));
    }

    @Test
    void anAaptaWhoHasntOpenedTheHourCantBeChosen() throws Exception {
        Booking booking = booking(soon, "Farah Ali");
        Aapta busy = aapta("Not", "Open");

        save(booking.getId(), busy.getId(), false, version(booking.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AAPTA_NOT_AVAILABLE"));
    }

    @Test
    void bookingsMoreThan24HoursAheadAreViewOnly() throws Exception {
        LocalDateTime farAhead = soon.plusHours(27);
        Booking booking = booking(farAhead, "Kabir Nair");
        openAt(anika, farAhead);

        save(booking.getId(), anika.getId(), false, version(booking.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_EDITABLE"));
    }

    @Test
    void aStaleVersionIsRejected() throws Exception {
        Booking booking = booking(soon, "Farah Ali");
        long loaded = version(booking.getId());
        save(booking.getId(), anika.getId(), false, loaded).andExpect(status().isOk());

        save(booking.getId(), ravi.getId(), false, loaded)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    @Test
    void theFormShowsWhatTheClientEntered() throws Exception {
        Booking booking = booking(soon, "Farah Ali");

        mvc.perform(get("/api/admin/bookings/{id}/form", booking.getId()).contextPath("/api").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Farah Ali"))
                .andExpect(jsonPath("$.email").value("farah-ali@example.com"))
                .andExpect(jsonPath("$.notes").value("Wanted to talk things through."))
                .andExpect(jsonPath("$.consent").value(true))
                .andExpect(jsonPath("$.submittedAt").isNumber())
                .andExpect(jsonPath("$.timezone").doesNotExist());
    }

    @Test
    void onlyAdminsCanSeeOrChangeBookingsHere() throws Exception {
        Booking booking = booking(soon, "Farah Ali");

        mvc.perform(get("/api/admin/bookings").contextPath("/api").param("date", calendar.dateOf(soon).toString())
                        .cookie(session()))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/bookings/{id}", booking.getId()).contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aaptaId\":\"%s\",\"bccEmailSent\":false,\"version\":0}".formatted(anika.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ── cancellation ─────────────────────────────────────────

    @Test
    void cancellingFreesTheHourAndKeepsTheReasonWhateverTheDate() throws Exception {
        LocalDateTime nextWeek = soon.plusDays(7);
        openAt(anika, nextWeek);
        Booking booking = booking(nextWeek, "Kabir Nair");
        Slot slot = slotRepository.findForAaptaAt(anika.getId(), nextWeek).orElseThrow();
        booking.allocateTo(slot, calendar.nowUtc());
        // Outside a transaction here, so the slot (not cascaded from the booking) is saved explicitly.
        slotRepository.save(slot);
        bookingRepository.save(booking);
        assertThat(slotRepository.findForAaptaAt(anika.getId(), nextWeek).orElseThrow().getStatus()).isEqualTo(SlotStatus.BOOKED);

        cancel(booking.getId(), "Client asked to call it off.", version(booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Client asked to call it off."))
                .andExpect(jsonPath("$.cancellable").value(false))
                .andExpect(jsonPath("$.editable").value(false));

        assertThat(slotRepository.findForAaptaAt(anika.getId(), nextWeek).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.AAPTA_AVAILABLE);
        dayOf(nextWeek)
                .andExpect(row(booking, "bookingStatus").value("CANCELLED"))
                .andExpect(row(booking, "cancelReason").value("Client asked to call it off."));
    }

    @Test
    void anUnallocatedBookingCanBeCancelledToo() throws Exception {
        Booking booking = booking(soon, "Farah Ali");

        cancel(booking.getId(), "Duplicate request.", version(booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
    }

    @Test
    void aMarkedOrAlreadyCancelledBookingCantBeCancelled() throws Exception {
        Booking completed = booking(soon.minusDays(2), "Sara Lopez");
        completed.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(completed);
        Booking cancelled = booking(soon, "Omar Farouk");
        cancel(cancelled.getId(), "First time.", version(cancelled.getId())).andExpect(status().isOk());

        cancel(completed.getId(), "Too late.", version(completed.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_CANCELLABLE"));
        cancel(cancelled.getId(), "Again.", version(cancelled.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_CANCELLABLE"));
    }

    @Test
    void aReasonIsRequiredAndAStaleVersionIsRejected() throws Exception {
        Booking booking = booking(soon, "Farah Ali");
        long loaded = version(booking.getId());

        cancel(booking.getId(), "   ", loaded)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        save(booking.getId(), anika.getId(), false, loaded).andExpect(status().isOk());
        cancel(booking.getId(), "Changed my mind.", loaded)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    // ── helpers ──────────────────────────────────────────────

    private Aapta aapta(String first, String last) {
        String email = "%s.%s.%s@example.com".formatted(first, last, UUID.randomUUID()).toLowerCase();
        return aaptaRepository.save(new Aapta(first, last, email, passwordEncoder.encode(DEMO_PASSWORD)));
    }

    private Aapta openAt(Aapta aapta, LocalDateTime start) {
        slotRepository.save(new Slot(aapta, start, SlotStatus.AAPTA_AVAILABLE));
        return aapta;
    }

    /** A booking as the public site would create it: no aapta yet. */
    private Booking booking(LocalDateTime start, String clientName) {
        return bookingRepository.save(Booking.builder()
                .customerName(clientName)
                .customerEmail(clientName.toLowerCase().replace(' ', '-') + "@example.com")
                .bookingReason("Wanted to talk things through.")
                .consentGivenAt(calendar.nowUtc())
                .startTime(start)
                .endTime(start.plusHours(1))
                .build());
    }

    private Slot slotOf(Aapta aapta) {
        return slotRepository.findForAaptaAt(aapta.getId(), soon).orElseThrow();
    }

    private long version(UUID bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow().getVersion();
    }

    private ResultActions dayOf(LocalDateTime utc) throws Exception {
        return mvc.perform(get("/api/admin/bookings").contextPath("/api")
                .param("date", calendar.dateOf(utc).toString())
                .cookie(admin));
    }

    /** A field of this booking's row in the day view. A JSONPath filter yields a one-item list, which {@code value(x)} unwraps. */
    private static JsonPathResultMatchers row(Booking booking, String field) {
        return jsonPath("$.bookings[?(@.bookingId == '%s')].%s".formatted(booking.getId(), field));
    }

    private ResultActions save(UUID bookingId, UUID aaptaId, boolean bcc, long version) throws Exception {
        return mvc.perform(patch("/api/admin/bookings/{id}", bookingId).contextPath("/api").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aaptaId\":\"%s\",\"bccEmailSent\":%s,\"version\":%d}".formatted(aaptaId, bcc, version)));
    }

    private ResultActions cancel(UUID bookingId, String reason, long version) throws Exception {
        return mvc.perform(post("/api/admin/bookings/{id}/cancel", bookingId).contextPath("/api").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"%s\",\"version\":%d}".formatted(reason, version)));
    }

    private void assertBookingVisibleTo(Aapta aapta, Booking booking, boolean visible) throws Exception {
        String body = mvc.perform(get("/api/bookings").contextPath("/api").cookie(session(aapta.getEmail()))
                        .param("startTime", String.valueOf(EpochTime.toMillis(soon.minusHours(1))))
                        .param("endTime", String.valueOf(EpochTime.toMillis(soon.plusHours(1)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.bookings[*].bookingId");
        assertThat(ids.contains(booking.getId().toString())).isEqualTo(visible);
    }
}
