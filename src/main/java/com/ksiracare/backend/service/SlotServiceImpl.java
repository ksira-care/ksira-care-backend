package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.ApiException;
import com.ksiracare.backend.exception.ErrorCode;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.exception.SlotConflictException;
import com.ksiracare.backend.mapper.SlotMapper;
import com.ksiracare.backend.policy.SlotSchedulePolicy;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    /** Aaptas open and close hours; only bookings make an hour BOOKED. */
    private static final Set<SlotStatus> AAPTA_SETTABLE =
            Set.of(SlotStatus.AAPTA_AVAILABLE, SlotStatus.AAPTA_UNAVAILABLE);

    private final AaptaRepository aaptaRepository;
    private final SlotRepository slotRepository;
    private final SlotSchedulePolicy schedulePolicy;
    private final SlotMapper slotMapper;
    private final PortalCalendar calendar;

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponseDto> getSlots(UUID aaptaId, UtcRange range) {
        Map<LocalDateTime, Slot> stored = storedSlots(aaptaId, range);
        return schedulePolicy.slotStartsWithin(range).stream()
                .map(start -> stored.containsKey(start)
                        ? slotMapper.toResponse(stored.get(start))
                        : new SlotResponseDto(null, EpochTime.toMillis(start), SlotStatus.AAPTA_UNAVAILABLE))
                .toList();
    }

    @Override
    @Transactional
    public List<SlotResponseDto> saveSlots(UUID aaptaId, UtcRange range, List<SlotRequestDto> changes) {
        if (!changes.isEmpty()) {
            LocalDateTime now = calendar.nowUtc();
            changes.forEach(change -> validate(change, range, now));
            rejectDuplicates(changes);

            Aapta aapta = aaptaRepository.getReferenceById(aaptaId);
            Map<LocalDateTime, Slot> stored = storedSlots(aaptaId, range);

            List<Slot> toSave = changes.stream().map(change -> {
                LocalDateTime start = EpochTime.toUtc(change.getTime());
                Slot slot = stored.get(start);
                if (slot == null) {
                    return new Slot(aapta, start, change.getStatus());
                }
                if (slot.getStatus() == SlotStatus.BOOKED) {
                    throw new SlotConflictException("A session is already booked at this time.");
                }
                slot.setStatus(change.getStatus());
                return slot;
            }).toList();
            slotRepository.saveAll(toSave);
        }
        return getSlots(aaptaId, range);
    }

    private void validate(SlotRequestDto change, UtcRange range, LocalDateTime now) {
        if (!AAPTA_SETTABLE.contains(change.getStatus())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Hours can only be opened or closed.");
        }
        LocalDateTime start = EpochTime.toUtc(change.getTime());
        if (!range.contains(start) || !schedulePolicy.isSlotStart(start)) {
            throw new InvalidSlotTimeException("That isn't a bookable hour.");
        }
        if (!schedulePolicy.isOpenForChanges(start, now)) {
            throw new InvalidSlotTimeException("That hour has already started or is too far ahead.");
        }
    }

    private static void rejectDuplicates(List<SlotRequestDto> changes) {
        Set<Long> seen = new HashSet<>();
        for (SlotRequestDto change : changes) {
            if (!seen.add(change.getTime())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "The same hour was sent twice.");
            }
        }
    }

    private Map<LocalDateTime, Slot> storedSlots(UUID aaptaId, UtcRange range) {
        return slotRepository.findForAapta(aaptaId, range.start(), range.end()).stream()
                .collect(Collectors.toMap(Slot::getSlotTime, Function.identity()));
    }
}
