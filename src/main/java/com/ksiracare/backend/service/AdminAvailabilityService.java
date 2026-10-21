package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.AaptaAvailabilityDto;
import com.ksiracare.backend.dto.response.AvailabilityCellDto;
import com.ksiracare.backend.dto.response.AvailabilityCellDto.BookedSessionDto;
import com.ksiracare.backend.dto.response.AvailabilityOverviewDto;
import com.ksiracare.backend.dto.response.AvailabilityOverviewDto.HourDto;
import com.ksiracare.backend.dto.response.ClientDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.policy.SlotSchedulePolicy;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The admin console's availability grid: every aapta's hours on one day, as they set them in
 * their portal. Read-only — allocation happens on the bookings (Today) page.
 */
@Service
@RequiredArgsConstructor
public class AdminAvailabilityService {

    private static final Comparator<Aapta> BY_NAME =
            Comparator.comparing(Aapta::getFirstName).thenComparing(Aapta::getLastName);

    private final AaptaRepository aaptaRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final SlotSchedulePolicy schedulePolicy;
    private final PortalCalendar calendar;

    @Transactional(readOnly = true)
    public AvailabilityOverviewDto getDay(LocalDate date) {
        UtcRange day = calendar.dayOf(date);
        List<LocalDateTime> hours = schedulePolicy.slotStartsWithin(day);
        List<Slot> recorded = slotRepository.findStartingBetween(day.start(), day.end());
        Map<UUID, Booking> bookingBySlot = bookingRepository.findAllocatedStartingBetween(day.start(), day.end()).stream()
                .collect(Collectors.toMap(booking -> booking.getSlot().getId(), Function.identity()));

        // Aapta → start → their slot. An hour with no record is one they never opened.
        Map<UUID, Map<LocalDateTime, Slot>> slotsByAapta = recorded.stream().collect(Collectors.groupingBy(
                slot -> slot.getAapta().getId(), Collectors.toMap(Slot::getSlotTime, Function.identity())));

        List<AaptaAvailabilityDto> rows = rowAaptas(recorded).stream()
                .map(aapta -> row(aapta, hours, slotsByAapta.getOrDefault(aapta.getId(), Map.of()), bookingBySlot))
                .toList();
        return new AvailabilityOverviewDto(date, columns(hours, rows), rows);
    }

    /** Active aaptas, plus anyone deactivated since who still had a booking that day — so past days stay accurate. */
    private Set<Aapta> rowAaptas(List<Slot> recorded) {
        Set<Aapta> aaptas = new LinkedHashSet<>(aaptaRepository.findAllByActiveTrue());
        recorded.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.BOOKED)
                .map(Slot::getAapta)
                .forEach(aaptas::add);
        return aaptas.stream().sorted(BY_NAME).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static AaptaAvailabilityDto row(Aapta aapta, List<LocalDateTime> hours,
                                            Map<LocalDateTime, Slot> slots, Map<UUID, Booking> bookingBySlot) {
        List<AvailabilityCellDto> cells = hours.stream().map(start -> {
            Slot slot = slots.get(start);
            if (slot == null) {
                return new AvailabilityCellDto(EpochTime.toMillis(start), SlotStatus.AAPTA_UNAVAILABLE, null);
            }
            Booking booking = bookingBySlot.get(slot.getId());
            BookedSessionDto session = slot.getStatus() == SlotStatus.BOOKED && booking != null
                    ? new BookedSessionDto(booking.getId(), new ClientDto(booking.getCustomerName(), booking.getCustomerEmail()))
                    : null;
            return new AvailabilityCellDto(EpochTime.toMillis(start), slot.getStatus(), session);
        }).toList();
        return new AaptaAvailabilityDto(aapta.getId(), aapta.getFirstName(), aapta.getLastName(), cells);
    }

    /** The column headers, each with how many aaptas are open at that hour. */
    private static List<HourDto> columns(List<LocalDateTime> hours, List<AaptaAvailabilityDto> rows) {
        return IntStream.range(0, hours.size())
                .mapToObj(i -> new HourDto(EpochTime.toMillis(hours.get(i)), (int) rows.stream()
                        .filter(row -> row.slots().get(i).status() == SlotStatus.AAPTA_AVAILABLE)
                        .count()))
                .toList();
    }
}
