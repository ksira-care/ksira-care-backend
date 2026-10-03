package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.exception.SlotConflictException;
import com.ksiracare.backend.mapper.SlotMapper;
import com.ksiracare.backend.policy.SlotSchedulePolicy;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.repository.TherapistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {

    private final TherapistRepository therapistRepository;
    private final SlotRepository slotRepository;
    private final SlotSchedulePolicy slotSchedulePolicy;
    private final SlotMapper slotMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponseDto> getSlots(UUID therapistId, Long startTime, Long endTime) {
        validateInputs(therapistId, startTime, endTime);

        LocalDateTime startDateTime = slotMapper.toLocalDateTime(startTime);
        LocalDateTime endDateTime = slotMapper.toLocalDateTime(endTime);
        validateDateOrder(startDateTime, endDateTime);

        if (!therapistRepository.existsById(therapistId)) {
            throw new ResourceNotFoundException("Therapist Not Found!");
        }

        List<LocalDateTime> fixedSlotTimes = slotSchedulePolicy.generateFixedSlotTimes(startDateTime, endDateTime);
        List<Slot> existingSlots = slotRepository.findByTherapistIdAndSlotTimeBetween(therapistId, startDateTime, endDateTime);

        Map<LocalDateTime, Slot> slotByTimeMap = existingSlots.stream()
                .collect(Collectors.toMap(Slot::getSlotTime, Function.identity(), (s1, s2) -> s1));

        List<SlotResponseDto> response = new ArrayList<>();
        for (LocalDateTime slotTime : fixedSlotTimes) {
            Slot existingSlot = slotByTimeMap.get(slotTime);
            if (existingSlot != null) {
                response.add(slotMapper.slotToResponseDto(existingSlot));
            } else {
                response.add(SlotResponseDto.builder()
                        .id(null)
                        .time(slotMapper.toEpochMilli(slotTime))
                        .status(SlotStatus.THERAPIST_UNAVAILABLE)
                        .build());
            }
        }

        return response;
    }

    @Override
    @Transactional
    public List<SlotResponseDto> saveSlots(UUID therapistId,
                                           Long startTime,
                                           Long endTime,
                                           List<SlotRequestDto> slotRequests) {
        validateInputs(therapistId, startTime, endTime);

        LocalDateTime startDateTime = slotMapper.toLocalDateTime(startTime);
        LocalDateTime endDateTime = slotMapper.toLocalDateTime(endTime);
        validateDateOrder(startDateTime, endDateTime);

        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist Not Found!"));

        if (slotRequests == null || slotRequests.isEmpty()) {
            return getSlots(therapistId, startTime, endTime);
        }

        Set<LocalDateTime> seenTimes = new HashSet<>();
        for (SlotRequestDto request : slotRequests) {
            if (request.getTime() == null) {
                throw new IllegalArgumentException("Slot time is required");
            }
            if (request.getStatus() == null) {
                throw new IllegalArgumentException("Slot status is required");
            }

            LocalDateTime requestDateTime = slotMapper.toLocalDateTime(request.getTime());

            if (!slotSchedulePolicy.isValidSlotTime(requestDateTime)) {
                throw new InvalidSlotTimeException("Invalid slot time: " + requestDateTime
                        + " (epoch: " + request.getTime() + "). Slots must be at the top of the hour between 9:00 AM and 11:00 PM UTC.");
            }
            if (requestDateTime.isBefore(startDateTime) || requestDateTime.isAfter(endDateTime)) {
                throw new InvalidSlotTimeException("Slot time " + requestDateTime
                        + " is outside the requested range [" + startDateTime + ", " + endDateTime + "]");
            }
            if (!seenTimes.add(requestDateTime)) {
                throw new IllegalArgumentException("Duplicate slot time in request: " + request.getTime());
            }
        }

        List<Slot> existingSlots = slotRepository.findByTherapistIdAndSlotTimeBetween(therapistId, startDateTime, endDateTime);
        Map<LocalDateTime, Slot> existingMap = existingSlots.stream()
                .collect(Collectors.toMap(Slot::getSlotTime, Function.identity(), (s1, s2) -> s1));

        List<Slot> toPersist = new ArrayList<>();
        for (SlotRequestDto request : slotRequests) {
            LocalDateTime requestDateTime = slotMapper.toLocalDateTime(request.getTime());
            Slot existing = existingMap.get(requestDateTime);
            if (existing != null) {
                if (existing.getStatus() == SlotStatus.BOOKED && request.getStatus() != SlotStatus.BOOKED) {
                    throw new SlotConflictException("Cannot modify already booked slot at: " + requestDateTime);
                }
                existing.setStatus(request.getStatus());
                toPersist.add(existing);
            } else {
                Slot newSlot = new Slot(therapist, requestDateTime, request.getStatus());
                toPersist.add(newSlot);
            }
        }

        slotRepository.saveAll(toPersist);

        return getSlots(therapistId, startTime, endTime);
    }

    private void validateInputs(UUID therapistId, Long startTime, Long endTime) {
        if (therapistId == null) {
            throw new IllegalArgumentException("therapistId is required");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
    }

    private void validateDateOrder(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("startTime must not be after endTime");
        }
    }
}
