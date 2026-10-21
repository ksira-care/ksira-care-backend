package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.AdminBookingUpdateRequestDto;
import com.ksiracare.backend.dto.response.AaptaRefDto;
import com.ksiracare.backend.dto.response.AdminBookingDto;
import com.ksiracare.backend.dto.response.BookingFormDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.BookingEvent;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.AaptaNotAvailableException;
import com.ksiracare.backend.exception.ApiException;
import com.ksiracare.backend.exception.ErrorCode;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.AdminBookingMapper;
import com.ksiracare.backend.policy.AdminBookingPolicy;
import com.ksiracare.backend.repository.BookingEventRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.security.Role;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The admin console's bookings: see a day, allocate or change the aapta, record the BCC email.
 * When changes are allowed is decided by {@link AdminBookingPolicy}; outcomes are the aapta's.
 * Admins see every booking, cancelled ones included: the row's status tells them apart.
 */
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final BookingEventRepository eventRepository;
    private final AdminBookingPolicy policy;
    private final AdminBookingMapper mapper;
    private final PortalCalendar calendar;

    /** Every booking starting on {@code date} (portal time zone), in start order, cancelled ones included. */
    @Transactional(readOnly = true)
    public List<AdminBookingDto> getDay(LocalDate date) {
        UtcRange day = calendar.dayOf(date);
        List<Booking> bookings = bookingRepository.findStartingBetween(day.start(), day.end());
        Map<UUID, Aapta> previous = previousAaptas(bookings.stream().map(Booking::getId).toList());
        LocalDateTime now = calendar.nowUtc();
        return bookings.stream().map(booking -> toRow(booking, previous.get(booking.getId()), now)).toList();
    }

    @Transactional(readOnly = true)
    public BookingFormDto getForm(UUID bookingId) {
        return mapper.toForm(find(bookingId));
    }

    /** The current aapta (if any), then every active aapta who opened that hour. */
    @Transactional(readOnly = true)
    public List<AaptaRefDto> getAaptaOptions(UUID bookingId) {
        Booking booking = find(bookingId);
        List<AaptaRefDto> options = new ArrayList<>();
        if (booking.getAapta() != null) {
            options.add(mapper.toRef(booking.getAapta()));
        }
        slotRepository.findAtTimeWithStatus(booking.getStartTime(), SlotStatus.AAPTA_AVAILABLE).stream()
                .map(slot -> mapper.toRef(slot.getAapta()))
                .forEach(options::add);
        return options;
    }

    /**
     * "Add" and "Update": gives the booking to {@code aaptaId} (if that's a change) and sets the
     * BCC flag. Each change is recorded in the booking's history.
     */
    @Transactional
    public AdminBookingDto update(AdminPrincipal admin, UUID bookingId, AdminBookingUpdateRequestDto request) {
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.getVersion(), request.version())) {
            throw new ApiException(ErrorCode.CONCURRENT_UPDATE,
                    "This booking was changed by someone else. Please reload and try again.");
        }
        LocalDateTime now = calendar.nowUtc();
        policy.checkEditable(booking, now);

        Aapta current = booking.getAapta();
        if (current == null || !current.getId().equals(request.aaptaId())) {
            Slot slot = openSlot(request.aaptaId(), booking.getStartTime());
            booking.allocateTo(slot, now);
            record(booking, current == null ? BookingEventType.AAPTA_ALLOCATED : BookingEventType.AAPTA_REALLOCATED,
                    admin, current, slot.getAapta(), now);
        }
        if (booking.isBccEmailSent() != request.bccEmailSent()) {
            booking.setBccEmailSent(request.bccEmailSent());
            record(booking, request.bccEmailSent() ? BookingEventType.BCC_EMAIL_MARKED_SENT : BookingEventType.BCC_EMAIL_MARKED_NOT_SENT,
                    admin, null, booking.getAapta(), now);
        }

        Booking saved = bookingRepository.saveAndFlush(booking);
        return toRow(saved, previousAaptas(List.of(saved.getId())).get(saved.getId()), now);
    }

    private Slot openSlot(UUID aaptaId, LocalDateTime start) {
        return slotRepository.findForAaptaAt(aaptaId, start)
                .filter(slot -> slot.getStatus() == SlotStatus.AAPTA_AVAILABLE && slot.getAapta().isActive())
                .orElseThrow(() -> new AaptaNotAvailableException("That aapta hasn't opened this hour, or it's no longer free."));
    }

    private void record(Booking booking, BookingEventType type, AdminPrincipal admin,
                        Aapta previousAapta, Aapta aapta, LocalDateTime now) {
        eventRepository.save(BookingEvent.builder()
                .booking(booking)
                .type(type)
                .actorRole(Role.ADMIN)
                .actorId(admin.adminId())
                .previousAapta(previousAapta)
                .aapta(aapta)
                .occurredAt(now)
                .build());
    }

    /** Booking id → who held it before its latest reallocation. */
    private Map<UUID, Aapta> previousAaptas(Collection<UUID> bookingIds) {
        Map<UUID, Aapta> previous = new HashMap<>();
        if (!bookingIds.isEmpty()) {
            // Newest first, so the first event seen per booking is its latest.
            eventRepository.findForBookings(bookingIds, BookingEventType.AAPTA_REALLOCATED)
                    .forEach(event -> previous.putIfAbsent(event.getBooking().getId(), event.getPreviousAapta()));
        }
        return previous;
    }

    private AdminBookingDto toRow(Booking booking, Aapta previousAapta, LocalDateTime now) {
        return mapper.toRow(booking, previousAapta, policy.isEditable(booking, now), policy.editableFrom(booking));
    }

    private Booking find(UUID bookingId) {
        return bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
    }
}
