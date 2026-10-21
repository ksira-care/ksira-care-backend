package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.AdminBookingUpdateRequestDto;
import com.ksiracare.backend.dto.request.AdminCancelBookingRequestDto;
import com.ksiracare.backend.dto.response.AaptaRefDto;
import com.ksiracare.backend.dto.response.AdminBookingDto;
import com.ksiracare.backend.dto.response.BookingFormDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.AaptaNotAvailableException;
import com.ksiracare.backend.exception.ApiException;
import com.ksiracare.backend.exception.ErrorCode;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.AdminBookingMapper;
import com.ksiracare.backend.policy.AaptaMatchPolicy;
import com.ksiracare.backend.policy.AdminBookingPolicy;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The admin console's bookings: see a day, allocate or change the aapta, record the BCC email,
 * cancel. What is allowed when is decided by {@link AdminBookingPolicy}; outcomes are the aapta's.
 * Admins see every booking, cancelled ones included: the row's status tells them apart.
 */
@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final BookingHistory history;
    private final AdminBookingPolicy policy;
    private final AaptaMatchPolicy matchPolicy;
    private final AdminBookingMapper mapper;
    private final PortalCalendar calendar;

    /** Every booking starting on {@code date} (portal time zone), in start order, cancelled ones included. */
    @Transactional(readOnly = true)
    public List<AdminBookingDto> getDay(LocalDate date) {
        UtcRange day = calendar.dayOf(date);
        List<Booking> bookings = bookingRepository.findStartingBetween(day.start(), day.end());
        Map<UUID, BookingHistory.Summary> histories = history.summarise(bookings.stream().map(Booking::getId).toList());
        LocalDateTime now = calendar.nowUtc();
        return bookings.stream().map(booking -> toRow(booking, histories.get(booking.getId()), now)).toList();
    }

    @Transactional(readOnly = true)
    public BookingFormDto getForm(UUID bookingId) {
        return mapper.toForm(find(bookingId));
    }

    /**
     * The current aapta (if any), then every active aapta who opened that hour and speaks one of
     * the client's languages.
     */
    @Transactional(readOnly = true)
    public List<AaptaRefDto> getAaptaOptions(UUID bookingId) {
        Booking booking = find(bookingId);
        List<AaptaRefDto> options = new ArrayList<>();
        if (booking.getAapta() != null) {
            options.add(mapper.toRef(booking.getAapta()));
        }
        slotRepository.findAtTimeWithStatus(booking.getStartTime(), SlotStatus.AAPTA_AVAILABLE).stream()
                .map(Slot::getAapta)
                .filter(aapta -> speaksClientsLanguage(aapta, booking))
                .map(mapper::toRef)
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
        checkVersion(booking, request.version());
        LocalDateTime now = calendar.nowUtc();
        policy.checkEditable(booking, now);

        Aapta current = booking.getAapta();
        if (current == null || !current.getId().equals(request.aaptaId())) {
            Slot slot = openSlot(request.aaptaId(), booking);
            booking.allocateTo(slot, now);
            history.record(booking, current == null ? BookingEventType.AAPTA_ALLOCATED : BookingEventType.AAPTA_REALLOCATED,
                    admin, current, slot.getAapta(), now, null);
        }
        if (booking.isBccEmailSent() != request.bccEmailSent()) {
            booking.setBccEmailSent(request.bccEmailSent());
            history.record(booking, request.bccEmailSent() ? BookingEventType.BCC_EMAIL_MARKED_SENT : BookingEventType.BCC_EMAIL_MARKED_NOT_SENT,
                    admin, null, booking.getAapta(), now, null);
        }
        return saveAndShow(booking, now);
    }

    /**
     * Calls the session off, at any time, as long as the aapta hasn't marked it. Their hour opens
     * again; the reason is kept in the booking's history. There is no undo.
     */
    @Transactional
    public AdminBookingDto cancel(AdminPrincipal admin, UUID bookingId, AdminCancelBookingRequestDto request) {
        Booking booking = find(bookingId);
        checkVersion(booking, request.version());
        policy.checkCancellable(booking);

        LocalDateTime now = calendar.nowUtc();
        booking.cancel();
        history.record(booking, BookingEventType.BOOKING_CANCELLED, admin, null, booking.getAapta(), now, request.reason().trim());
        return saveAndShow(booking, now);
    }

    private static void checkVersion(Booking booking, Long loadedVersion) {
        if (!Objects.equals(booking.getVersion(), loadedVersion)) {
            throw new ApiException(ErrorCode.CONCURRENT_UPDATE,
                    "This booking was changed by someone else. Please reload and try again.");
        }
    }

    private AdminBookingDto saveAndShow(Booking booking, LocalDateTime now) {
        Booking saved = bookingRepository.saveAndFlush(booking);
        return toRow(saved, history.summarise(List.of(saved.getId())).get(saved.getId()), now);
    }

    private Slot openSlot(UUID aaptaId, Booking booking) {
        Slot slot = slotRepository.findForAaptaAt(aaptaId, booking.getStartTime())
                .filter(open -> open.getStatus() == SlotStatus.AAPTA_AVAILABLE && open.getAapta().isActive())
                .orElseThrow(() -> new AaptaNotAvailableException("That aapta hasn't opened this hour, or it's no longer free."));
        if (!speaksClientsLanguage(slot.getAapta(), booking)) {
            throw new AaptaNotAvailableException("That aapta doesn't speak any of the client's languages.");
        }
        return slot;
    }

    private boolean speaksClientsLanguage(Aapta aapta, Booking booking) {
        return matchPolicy.canTake(LanguageEntity.codes(aapta.getLanguages()), LanguageEntity.codes(booking.getCustomerPreferredLanguages()));
    }


    private AdminBookingDto toRow(Booking booking, BookingHistory.Summary summary, LocalDateTime now) {
        return mapper.toRow(booking, summary.previousAapta(), summary.cancelReason(),
                policy.isEditable(booking, now), policy.editableFrom(booking), policy.isCancellable(booking));
    }

    private Booking find(UUID bookingId) {
        return bookingRepository.findWithDetailsById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found."));
    }
}
