package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.CreateAaptaRequestDto;
import com.ksiracare.backend.dto.response.AaptaRemovedDto;
import com.ksiracare.backend.dto.response.AaptaSummaryDto;
import com.ksiracare.backend.dto.response.LanguageDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.EmailTakenException;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.mapper.AaptaMapper;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.LanguageRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The admin roster of aaptas: list, add (the admin sets their login), remove.
 * <p>
 * Removing is a soft delete — bookings, hours and history keep pointing at the aapta — so it
 * deactivates the account: the login stops at once, upcoming sessions go back to unallocated,
 * and their future open hours are closed.
 */
@Service
@RequiredArgsConstructor
public class AdminAaptaService {

    private static final String EMAIL_TAKEN = "An account with this email already exists (it may belong to a removed aapta).";

    private final AaptaRepository aaptaRepository;
    private final LanguageRepository languageRepository;
    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final BookingHistory history;
    private final AaptaMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final PortalCalendar calendar;

    @Transactional(readOnly = true)
    public List<AaptaSummaryDto> getRoster() {
        return aaptaRepository.findActiveWithLanguages().stream().map(mapper::toSummary).toList();
    }

    /** The languages aaptas can be given, by name. */
    @Transactional(readOnly = true)
    public List<LanguageDto> getLanguages() {
        return languageRepository.findAllByOrderByNameAsc().stream()
                .map(language -> new LanguageDto(language.getCode(), language.getName()))
                .toList();
    }

    @Transactional
    public AaptaSummaryDto create(CreateAaptaRequestDto request) {
        // Stored lower-case so the database's unique constraint also catches "Aanya@…" vs "aanya@…".
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (aaptaRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailTakenException(EMAIL_TAKEN);
        }
        Aapta aapta = new Aapta(request.firstName().trim(), request.lastName().trim(), email,
                passwordEncoder.encode(request.password()));
        aapta.setMiddleName(StringUtils.hasText(request.middleName()) ? request.middleName().trim() : null);
        aapta.setLanguages(new HashSet<>(languageRepository.findAllByCodeIn(request.languages())));
        try {
            return mapper.toSummary(aaptaRepository.saveAndFlush(aapta));
        } catch (DataIntegrityViolationException raceWithAnotherAdmin) {
            throw new EmailTakenException(EMAIL_TAKEN);
        }
    }

    /** Deactivates the aapta and hands their upcoming sessions back for allocation. */
    @Transactional
    public AaptaRemovedDto remove(AdminPrincipal admin, UUID aaptaId) {
        Aapta aapta = aaptaRepository.findById(aaptaId)
                .filter(Aapta::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Aapta not found."));
        LocalDateTime now = calendar.nowUtc();
        aapta.setActive(false);

        List<Booking> upcoming = bookingRepository.findForAaptaWithStatusStartingAfter(aaptaId, BookingStatus.PENDING, now);
        for (Booking booking : upcoming) {
            Slot held = booking.unallocate();
            if (held != null) {
                held.close();
            }
            history.record(booking, BookingEventType.AAPTA_UNALLOCATED, admin, aapta, null, now, "Aapta removed");
        }
        slotRepository.findForAaptaWithStatusStartingAfter(aaptaId, SlotStatus.AAPTA_AVAILABLE, now).forEach(Slot::close);
        return new AaptaRemovedDto(aaptaId, upcoming.size());
    }
}
