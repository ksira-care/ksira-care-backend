package com.ksiracare.backend.config;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.LanguageRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Demo data for local development only (dev profile, in-memory database), matching the
 * portal's mock accounts:
 * <ul>
 *   <li>aapta@ksiracare.com / password123 — active, with bookings and open hours</li>
 *   <li>disabled@ksiracare.com / password123 — deactivated</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "password123";
    private static final int SESSION_MINUTES = 60;

    private final AaptaRepository aaptaRepository;
    private final LanguageRepository languageRepository;
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortalCalendar calendar;

    private record DemoClient(String name, String reason, Language... languages) {
    }

    private static final DemoClient[] CLIENTS = {
            new DemoClient("Sara Lopez", "To be heard about work stress.", Language.ENGLISH),
            new DemoClient("Omar Farouk", "Anxious thoughts, wanted to slow down.", Language.ENGLISH, Language.HINDI),
            new DemoClient("Priya Raman", "Grief — just needed someone to listen.", Language.ENGLISH, Language.KANNADA),
            new DemoClient("Rohan Kulkarni", "Feeling stuck after moving cities.", Language.MARATHI, Language.HINDI, Language.ENGLISH),
            new DemoClient("Ishita Bose", "Good news and no one to tell!", Language.HINDI),
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (aaptaRepository.count() > 0) {
            return;
        }
        Aapta aanya = createAapta("Aanya", "Mehta", "aapta@ksiracare.com", true);
        aanya.setPhone("+919876543210");
        aanya.setDateOfBirth(LocalDate.of(1991, 3, 14));
        aanya.setAddress("Flat 402, Green Meadows, Pune 411014");
        aanya.setLanguages(languages(Language.ENGLISH, Language.HINDI, Language.MARATHI));
        createAapta("Dev", "Mehta", "disabled@ksiracare.com", false);

        seedBookingsAndHours(aanya);
        log.info("Seeded dev data — sign in with aapta@ksiracare.com / {}", DEMO_PASSWORD);
    }

    private Aapta createAapta(String first, String last, String email, boolean active) {
        Aapta aapta = new Aapta(first, last, email, passwordEncoder.encode(DEMO_PASSWORD));
        aapta.setActive(active);
        return aaptaRepository.save(aapta);
    }

    private void seedBookingsAndHours(Aapta aapta) {
        LocalDate today = calendar.today();
        LocalDateTime now = calendar.nowUtc();
        int clientIndex = 0;

        // Last 20 days: mostly completed, the odd no-show, yesterday's left unmarked.
        for (int daysAgo = 20; daysAgo >= 1; daysAgo--) {
            if (daysAgo % 3 == 0) {
                continue;
            }
            LocalDate day = today.minusDays(daysAgo);
            BookingStatus status = daysAgo == 1 ? BookingStatus.PENDING
                    : daysAgo % 7 == 2 ? BookingStatus.CLIENT_NO_SHOW : BookingStatus.COMPLETED;
            book(aapta, day, 11, CLIENTS[clientIndex++ % CLIENTS.length], status, now);
        }

        // Today: one earlier (unmarked), the rest still to come.
        for (int hour : new int[]{10, 14, 17, 20}) {
            book(aapta, today, hour, CLIENTS[clientIndex++ % CLIENTS.length], BookingStatus.PENDING, now);
        }

        // Tomorrow's first session was moved by an admin.
        Booking moved = book(aapta, today.plusDays(1), 10, CLIENTS[clientIndex++ % CLIENTS.length], BookingStatus.PENDING, now);
        moved.setRescheduled(true);
        moved.setPreviousStartTime(calendar.toUtc(today, LocalTime.of(16, 0)));
        moved.setRescheduleReason("Client requested the change by email more than 24 h ahead.");

        // Next two weeks: a few open hours on weekdays.
        for (int daysAhead = 1; daysAhead <= 14; daysAhead++) {
            LocalDate day = today.plusDays(daysAhead);
            if (day.getDayOfWeek().getValue() >= 6) {
                continue;
            }
            for (int hour : new int[]{9, 12, 13, 16, 19}) {
                LocalDateTime start = calendar.toUtc(day, LocalTime.of(hour, 0));
                if (slotRepository.findForAapta(aapta.getId(), start, start.plusMinutes(1)).isEmpty()) {
                    slotRepository.save(new Slot(aapta, start, SlotStatus.AAPTA_AVAILABLE));
                }
            }
        }
    }

    private Booking book(Aapta aapta, LocalDate day, int hour, DemoClient client,
                         BookingStatus status, LocalDateTime now) {
        LocalDateTime start = calendar.toUtc(day, LocalTime.of(hour, 0));
        Slot slot = slotRepository.save(new Slot(aapta, start, SlotStatus.BOOKED));
        LocalDateTime end = start.plusMinutes(SESSION_MINUTES);
        return bookingRepository.save(Booking.builder()
                .aapta(aapta)
                .slot(slot)
                .customerName(client.name())
                .bookingReason(client.reason())
                .customerPreferredLanguages(languages(client.languages()))
                .startTime(start)
                .endTime(end)
                .status(status)
                .assignedAt(start.minusDays(3))
                .markedAt(status == BookingStatus.PENDING ? null : end.plusMinutes(10).isBefore(now) ? end.plusMinutes(10) : now)
                .build());
    }

    private Set<LanguageEntity> languages(Language... codes) {
        return Arrays.stream(codes)
                .map(code -> languageRepository.findByCode(code).orElseThrow())
                .collect(Collectors.toSet());
    }
}
