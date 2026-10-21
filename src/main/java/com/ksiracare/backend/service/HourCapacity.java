package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.policy.AaptaMatchPolicy;
import com.ksiracare.backend.policy.SlotSchedulePolicy;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Whether another client fits at an hour: every booking still waiting for an aapta at that hour,
 * plus the new one, must be able to get an open aapta of their own who shares a language
 * ({@link AaptaMatchPolicy}). Allocated bookings already hold their aapta's hour, so they are not
 * counted again.
 */
@Component
@RequiredArgsConstructor
class HourCapacity {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final SlotSchedulePolicy schedule;
    private final AaptaMatchPolicy matchPolicy;

    /** Every session start in {@code range} where a client speaking {@code languages} would still fit. */
    List<LocalDateTime> hoursWithRoomFor(UtcRange range, Set<Language> languages) {
        Map<LocalDateTime, List<Set<Language>>> openAaptas = slotRepository
                .findOpenWithLanguagesStartingBetween(range.start(), range.end()).stream()
                .collect(Collectors.groupingBy(Slot::getSlotTime,
                        Collectors.mapping(slot -> languagesOf(slot.getAapta()), Collectors.toList())));
        Map<LocalDateTime, List<Set<Language>>> waiting = bookingRepository
                .findUnallocatedStartingBetween(range.start(), range.end()).stream()
                .collect(Collectors.groupingBy(Booking::getStartTime,
                        Collectors.mapping(HourCapacity::languagesOf, Collectors.toList())));

        return schedule.slotStartsWithin(range).stream()
                .filter(hour -> fits(languages, waiting.getOrDefault(hour, List.of()), openAaptas.getOrDefault(hour, List.of())))
                .toList();
    }

    /**
     * Whether a client speaking {@code languages} fits at {@code hour}. Locks that hour's open
     * slots until the caller's transaction ends, so concurrent bookings are decided one at a time.
     */
    boolean hasRoomFor(LocalDateTime hour, Set<Language> languages) {
        List<Set<Language>> openAaptas = slotRepository.lockOpenAt(hour).stream()
                .map(Slot::getAapta)
                .filter(Aapta::isActive)
                .map(HourCapacity::languagesOf)
                .toList();
        List<Set<Language>> waiting = bookingRepository.findUnallocatedStartingAt(hour).stream()
                .map(HourCapacity::languagesOf)
                .toList();
        return fits(languages, waiting, openAaptas);
    }

    private boolean fits(Set<Language> newClient, List<Set<Language>> waiting, List<Set<Language>> openAaptas) {
        List<Set<Language>> clients = new ArrayList<>(waiting);
        clients.add(newClient);
        return matchPolicy.canSeatAll(clients, openAaptas);
    }

    private static Set<Language> languagesOf(Aapta aapta) {
        return LanguageEntity.codes(aapta.getLanguages());
    }

    private static Set<Language> languagesOf(Booking booking) {
        return LanguageEntity.codes(booking.getCustomerPreferredLanguages());
    }

}
