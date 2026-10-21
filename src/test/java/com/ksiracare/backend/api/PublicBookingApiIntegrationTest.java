package com.ksiracare.backend.api;

import com.jayway.jsonpath.JsonPath;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.LanguageRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Booking from the website. Uses a day three weeks out, past the demo data's open hours, so the
 * only aaptas open that day are the ones each test sets up.
 */
class PublicBookingApiIntegrationTest extends ApiTestSupport {

    /** Each test books from its own address, so the per-client limit never carries over. */
    private static final AtomicInteger ADDRESSES = new AtomicInteger();

    @Autowired private AaptaRepository aaptaRepository;
    @Autowired private SlotRepository slotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private LanguageRepository languageRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PortalCalendar calendar;

    private LocalDate day;
    private String clientAddress;

    @BeforeEach
    void setUp() {
        // A fresh day per test: earlier tests' bookings don't take its hours.
        day = calendar.today().plusDays(21 + ADDRESSES.get() % 30);
        clientAddress = "203.0.113." + ADDRESSES.incrementAndGet();
    }

    @Test
    void offersOnlyHoursWhereSomeoneWhoSpeaksTheirLanguageIsFree() throws Exception {
        Aapta kannada = aapta(Language.KANNADA, Language.ENGLISH);
        Aapta marathi = aapta(Language.MARATHI);
        open(kannada, 10, 11);
        open(marathi, 12);

        availability("kn").andExpect(jsonPath("$.hours[*].startTime", contains(millis(10), millis(11))));
        availability("mr,kn").andExpect(jsonPath("$.hours[*].startTime", contains(millis(10), millis(11), millis(12))));
        availability("hi").andExpect(jsonPath("$.hours", empty()));
        availability("kn").andExpect(jsonPath("$.hours[0].endTime").value(millis(11)));
    }

    @Test
    void aBookingArrivesUnallocatedForTheAdmin() throws Exception {
        open(aapta(Language.KANNADA), 10);

        String body = book(10, "kn").andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value(millis(10)))
                .andReturn().getResponse().getContentAsString();

        Booking booking = bookingRepository.findById(UUID.fromString(JsonPath.read(body, "$.bookingId"))).orElseThrow();
        assertThat(booking.getAapta()).isNull();
        assertThat(booking.getCustomerEmail()).isEqualTo("tom.h@example.com");
        assertThat(booking.getConsentGivenAt()).isNotNull();
        mvc.perform(get("/api/admin/bookings/{id}/form", booking.getId()).contextPath("/api").cookie(adminSession()))
                .andExpect(jsonPath("$.name").value("Tom H"))
                .andExpect(jsonPath("$.consent").value(true));
    }

    @Test
    void anHourIsGoneOnceEverySpeakerOfTheLanguageIsSpokenFor() throws Exception {
        open(aapta(Language.KANNADA), 10);
        open(aapta(Language.MARATHI), 10);

        book(10, "kn").andExpect(status().isCreated());

        book(10, "kn").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIME_NOT_AVAILABLE"));
        availability("kn").andExpect(jsonPath("$.hours", empty()));
        book(10, "mr").andExpect(status().isCreated());
    }

    @Test
    void earlierClientsAreSeatedSoThatEveryoneFits() throws Exception {
        // The first client could go to either aapta; only one speaks Kannada.
        open(aapta(Language.KANNADA, Language.MARATHI), 10);
        open(aapta(Language.MARATHI), 10);

        book(10, "mr").andExpect(status().isCreated());

        book(10, "kn").andExpect(status().isCreated());
    }

    @Test
    void onlySessionHoursInsideTheWindowCanBeBooked() throws Exception {
        open(aapta(Language.KANNADA), 10);

        bookAt(EpochTime.toMillis(at(10).plusMinutes(30)), "kn").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_SLOT_TIME"));
        bookAt(EpochTime.toMillis(calendar.toUtc(calendar.today().minusDays(1), LocalTime.of(10, 0))), "kn")
                .andExpect(status().isUnprocessableContent());
        bookAt(EpochTime.toMillis(calendar.toUtc(calendar.today().plusDays(90), LocalTime.of(10, 0))), "kn")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void theFormIsValidated() throws Exception {
        for (String body : new String[]{
                bookingBody(millis(10), "[]", true),
                bookingBody(millis(10), "[\"kn\"]", false),
                bookingBody(millis(10), "[\"xx\"]", true),
                bookingBody(millis(10), "[\"kn\"]", true).replace("Tom H", "T"),
        }) {
            post(body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void oneAddressCanOnlyBookAFewTimesAnHour() throws Exception {
        open(aapta(Language.KANNADA), 10, 11, 12, 13, 14, 15);

        for (int hour = 10; hour < 15; hour++) {
            book(hour, "kn").andExpect(status().isCreated());
        }

        book(15, "kn").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void needsNoSignIn() throws Exception {
        availability("kn").andExpect(status().isOk());
    }

    // ── helpers ──────────────────────────────────────────────

    private Aapta aapta(Language... languages) {
        String email = "public.%s@example.com".formatted(UUID.randomUUID());
        Aapta aapta = new Aapta("Test", "Aapta", email, passwordEncoder.encode(DEMO_PASSWORD));
        aapta.setLanguages(languageRepository.findSet(List.of(languages)));
        return aaptaRepository.save(aapta);
    }

    private void open(Aapta aapta, int... istHours) {
        for (int hour : istHours) {
            slotRepository.save(new Slot(aapta, at(hour), SlotStatus.AAPTA_AVAILABLE));
        }
    }

    private LocalDateTime at(int istHour) {
        return calendar.toUtc(day, LocalTime.of(istHour, 0));
    }

    private long millis(int istHour) {
        return EpochTime.toMillis(at(istHour));
    }

    private ResultActions availability(String languages) throws Exception {
        return mvc.perform(get("/api/public/availability").contextPath("/api")
                .param("startTime", String.valueOf(millis(0)))
                .param("endTime", String.valueOf(millis(23) + 3_600_000))
                .param("languages", languages));
    }

    private ResultActions book(int istHour, String language) throws Exception {
        return bookAt(millis(istHour), language);
    }

    private ResultActions bookAt(long startTime, String language) throws Exception {
        return post(bookingBody(startTime, "[\"%s\"]".formatted(language), true));
    }

    private ResultActions post(String body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/public/bookings").contextPath("/api")
                .with(request -> {
                    request.setRemoteAddr(clientAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String bookingBody(long startTime, String languages, boolean consent) {
        return """
                {"name": "Tom H", "email": "Tom.H@Example.com", "startTime": %d,
                 "languages": %s, "notes": "Just wants someone to listen.", "consent": %s}
                """.formatted(startTime, languages, consent);
    }
}
