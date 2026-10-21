package com.ksiracare.backend.api;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Admin;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.AdminRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.PortalCalendar;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The admin roster: list, add and remove aaptas, and changing your own password. */
class AdminAaptaApiIntegrationTest extends ApiTestSupport {

    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PortalCalendar calendar;

    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminSession();
    }

    // ── roster and languages ─────────────────────────────────

    @Test
    void theRosterListsActiveAaptasWithLanguageCodes() throws Exception {
        mvc.perform(get("/api/admin/aaptas").contextPath("/api").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aaptas[?(@.email == 'aapta@ksiracare.com')].languages[*]",
                        containsInAnyOrder("en", "hi", "mr")))
                .andExpect(jsonPath("$.aaptas[*].email", not(hasItem("disabled@ksiracare.com"))))
                .andExpect(jsonPath("$.aaptas[0].password").doesNotExist());
    }

    @Test
    void theLanguagePickerListsEverySupportedLanguage() throws Exception {
        mvc.perform(get("/api/admin/languages").contextPath("/api").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languages[*].code", containsInAnyOrder("en", "hi", "mr", "kn")))
                .andExpect(jsonPath("$.languages[?(@.code == 'hi')].name").value("Hindi"));
    }

    // ── add ──────────────────────────────────────────────────

    @Test
    void anAddedAaptaCanSignInWithThePasswordTheAdminGaveThem() throws Exception {
        String email = unique("Rohan");

        create(email.toUpperCase(), "k7Rm2xQp9a", "[\"en\",\"hi\",\"mr\"]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.languages[*]", containsInAnyOrder("en", "hi", "mr")));

        assertThat(signIn(email, "k7Rm2xQp9a").getResponse().getStatus()).isEqualTo(200);
        mvc.perform(get("/api/admin/aaptas").contextPath("/api").cookie(admin))
                .andExpect(jsonPath("$.aaptas[*].email", hasItem(email)));
    }

    @Test
    void anEmailCanOnlyBeUsedOnceWhateverItsCase() throws Exception {
        String email = unique("Twice");
        create(email, "k7Rm2xQp9a", "[\"en\"]").andExpect(status().isCreated());

        create(email.toUpperCase(), "k7Rm2xQp9a", "[\"en\"]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void invalidDetailsAreRejected() throws Exception {
        create("not-an-email", "k7Rm2xQp9a", "[\"en\"]").andExpect(status().isBadRequest());
        create(unique("Short"), "short", "[\"en\"]").andExpect(status().isBadRequest());
        create(unique("NoLang"), "k7Rm2xQp9a", "[]").andExpect(status().isBadRequest());
        create(unique("BadLang"), "k7Rm2xQp9a", "[\"xx\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ── remove ───────────────────────────────────────────────

    @Test
    void removingStopsTheLoginAndHandsUpcomingSessionsBack() throws Exception {
        Aapta dev = aapta("Dev", "Patel");
        Cookie devSession = session(dev.getEmail());
        LocalDateTime now = calendar.nowUtc().truncatedTo(ChronoUnit.HOURS);
        Booking upcoming = allocated(dev, now.plusHours(5), BookingStatus.PENDING);
        Booking past = allocated(dev, now.minusDays(2), BookingStatus.COMPLETED);
        Slot laterOpen = slotRepository.save(new Slot(dev, now.plusDays(3), SlotStatus.AAPTA_AVAILABLE));

        mvc.perform(delete("/api/admin/aaptas/{id}", dev.getId()).contextPath("/api").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unallocatedBookings").value(1));

        // Login and the open session stop working.
        assertThat(signIn(dev.getEmail(), DEMO_PASSWORD).getResponse().getStatus()).isEqualTo(403);
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(devSession)).andExpect(status().isUnauthorized());

        // The upcoming session waits for a new aapta, showing who had it; the past one is untouched.
        Booking handedBack = bookingRepository.findWithDetailsById(upcoming.getId()).orElseThrow();
        assertThat(handedBack.getAapta()).isNull();
        assertThat(handedBack.getSlot()).isNull();
        assertThat(bookingRepository.findWithDetailsById(past.getId()).orElseThrow().getAapta()).isEqualTo(dev);
        mvc.perform(get("/api/admin/bookings").contextPath("/api").cookie(admin)
                        .param("date", calendar.dateOf(upcoming.getStartTime()).toString()))
                .andExpect(jsonPath("$.bookings[?(@.bookingId == '%s')].previousAapta.firstName".formatted(upcoming.getId()))
                        .value("Dev"));

        // Their hours are closed, and they're off the roster.
        assertThat(slotRepository.findForAaptaAt(dev.getId(), upcoming.getStartTime()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.AAPTA_UNAVAILABLE);
        assertThat(slotRepository.findById(laterOpen.getId()).orElseThrow().getStatus()).isEqualTo(SlotStatus.AAPTA_UNAVAILABLE);
        mvc.perform(get("/api/admin/aaptas").contextPath("/api").cookie(admin))
                .andExpect(jsonPath("$.aaptas[*].aaptaId", not(hasItem(dev.getId().toString()))));
        mvc.perform(delete("/api/admin/aaptas/{id}", dev.getId()).contextPath("/api").cookie(admin))
                .andExpect(status().isNotFound());
    }

    // ── change password ──────────────────────────────────────

    @Test
    void anAaptaCanChangeTheirPassword() throws Exception {
        Aapta neha = aapta("Neha", "Kapoor");
        Cookie session = session(neha.getEmail());

        changePassword("/api/auth/password", session, "wrong-one", "brand-new-pass")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INCORRECT"));
        changePassword("/api/auth/password", session, DEMO_PASSWORD, "short")
                .andExpect(status().isBadRequest());
        changePassword("/api/auth/password", session, DEMO_PASSWORD, "brand-new-pass")
                .andExpect(status().isNoContent());

        assertThat(signIn(neha.getEmail(), DEMO_PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(signIn(neha.getEmail(), "brand-new-pass").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void changingAPasswordSignsOutEverywhereExceptThisDevice() throws Exception {
        Aapta neha = aapta("Neha", "Kapoor");
        Cookie phone = session(neha.getEmail());
        Cookie laptop = session(neha.getEmail());

        Cookie refreshed = changePassword("/api/auth/password", laptop, DEMO_PASSWORD, "brand-new-pass")
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie(COOKIE);

        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(phone)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(laptop)).andExpect(status().isUnauthorized());
        assertThat(refreshed).isNotNull();
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(refreshed)).andExpect(status().isOk());
    }

    @Test
    void adminsCanChangeTheirOwnPasswordTheSameWay() throws Exception {
        Admin priya = adminRepository.save(new Admin("Priya", "Nair", unique("Priya"), passwordEncoder.encode(DEMO_PASSWORD)));
        Cookie other = adminSignIn(priya.getEmail(), DEMO_PASSWORD).getResponse().getCookie(ADMIN_COOKIE);
        Cookie current = adminSignIn(priya.getEmail(), DEMO_PASSWORD).getResponse().getCookie(ADMIN_COOKIE);

        Cookie refreshed = changePassword("/api/admin/auth/password", current, DEMO_PASSWORD, "brand-new-pass")
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie(ADMIN_COOKIE);

        mvc.perform(get("/api/admin/me").contextPath("/api").cookie(other)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/me").contextPath("/api").cookie(refreshed)).andExpect(status().isOk());
        assertThat(adminSignIn(priya.getEmail(), "brand-new-pass").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void changingAPasswordNeedsTheRightSession() throws Exception {
        changePassword("/api/auth/password", admin, DEMO_PASSWORD, "brand-new-pass").andExpect(status().isUnauthorized());
        changePassword("/api/admin/auth/password", session(), DEMO_PASSWORD, "brand-new-pass").andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────

    private static String unique(String name) {
        return "%s.%s@example.com".formatted(name, UUID.randomUUID()).toLowerCase();
    }

    private Aapta aapta(String first, String last) {
        return aaptaRepository.save(new Aapta(first, last, unique(first), passwordEncoder.encode(DEMO_PASSWORD)));
    }

    private Booking allocated(Aapta aapta, LocalDateTime start, BookingStatus status) {
        Slot slot = slotRepository.save(new Slot(aapta, start, SlotStatus.BOOKED));
        return bookingRepository.save(Booking.builder()
                .aapta(aapta).slot(slot)
                .customerName("Farah Ali").customerEmail("farah@example.com")
                .startTime(start).endTime(start.plusHours(1))
                .status(status)
                .build());
    }

    private ResultActions create(String email, String password, String languagesJson) throws Exception {
        return mvc.perform(post("/api/admin/aaptas").contextPath("/api").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "Rohan", "lastName": "Kulkarni", "email": "%s", "password": "%s", "languages": %s}
                        """.formatted(email, password, languagesJson)));
    }

    private ResultActions changePassword(String path, Cookie session, String current, String next) throws Exception {
        return mvc.perform(post(path).contextPath("/api").cookie(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
    }
}
