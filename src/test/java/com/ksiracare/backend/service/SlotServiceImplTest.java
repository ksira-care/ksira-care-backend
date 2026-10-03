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
import com.ksiracare.backend.policy.FixedHourlySlotSchedulePolicy;
import com.ksiracare.backend.policy.SlotSchedulePolicy;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.repository.TherapistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotServiceImplTest {

    @Mock
    private TherapistRepository therapistRepository;

    @Mock
    private SlotRepository slotRepository;

    private final SlotSchedulePolicy slotSchedulePolicy = new FixedHourlySlotSchedulePolicy();
    private final SlotMapper slotMapper = Mappers.getMapper(SlotMapper.class);

    private SlotServiceImpl slotService;

    private UUID therapistId;
    private Therapist mockTherapist;

    @BeforeEach
    void setUp() {
        slotService = new SlotServiceImpl(
                therapistRepository,
                slotRepository,
                slotSchedulePolicy,
                slotMapper
        );

        therapistId = UUID.randomUUID();
        mockTherapist = mock(Therapist.class);
        lenient().when(mockTherapist.getId()).thenReturn(therapistId);
    }

    private Long toEpochMilli(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute, 0)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when therapist does not exist")
    void testGetSlotsTherapistNotFound() {
        Long start = toEpochMilli(2026, 9, 27, 9, 0);
        Long end = toEpochMilli(2026, 9, 27, 12, 0);

        when(therapistRepository.existsById(therapistId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                slotService.getSlots(therapistId, start, end));
    }

    @Test
    @DisplayName("Should return slots with default THERAPIST_UNAVAILABLE when not persisted")
    void testGetSlotsDefaultUnavailable() {
        Long start = toEpochMilli(2026, 9, 27, 9, 0);
        Long end = toEpochMilli(2026, 9, 27, 11, 0);
        LocalDateTime startDT = LocalDateTime.of(2026, 9, 27, 9, 0);
        LocalDateTime endDT = LocalDateTime.of(2026, 9, 27, 11, 0);

        when(therapistRepository.existsById(therapistId)).thenReturn(true);
        when(slotRepository.findByTherapistIdAndSlotTimeBetween(therapistId, startDT, endDT))
                .thenReturn(List.of());

        List<SlotResponseDto> result = slotService.getSlots(therapistId, start, end);

        // 9:00, 10:00, 11:00 -> 3 slots
        assertEquals(3, result.size());
        for (SlotResponseDto slot : result) {
            assertNull(slot.getId());
            assertEquals(SlotStatus.THERAPIST_UNAVAILABLE, slot.getStatus());
        }
    }

    @Test
    @DisplayName("Should overlay persisted slots on the fixed grid")
    void testGetSlotsWithPersistedOverrides() {
        Long start = toEpochMilli(2026, 9, 27, 9, 0);
        Long end = toEpochMilli(2026, 9, 27, 11, 0);
        LocalDateTime startDT = LocalDateTime.of(2026, 9, 27, 9, 0);
        LocalDateTime endDT = LocalDateTime.of(2026, 9, 27, 11, 0);

        LocalDateTime bookedTime = LocalDateTime.of(2026, 9, 27, 10, 0);
        UUID bookedSlotId = UUID.randomUUID();
        Slot bookedSlot = Slot.builder()
                .id(bookedSlotId)
                .therapist(mockTherapist)
                .slotTime(bookedTime)
                .status(SlotStatus.BOOKED)
                .build();

        when(therapistRepository.existsById(therapistId)).thenReturn(true);
        when(slotRepository.findByTherapistIdAndSlotTimeBetween(therapistId, startDT, endDT))
                .thenReturn(List.of(bookedSlot));

        List<SlotResponseDto> result = slotService.getSlots(therapistId, start, end);

        assertEquals(3, result.size());
        // 9:00 -> default unavailable
        assertEquals(SlotStatus.THERAPIST_UNAVAILABLE, result.get(0).getStatus());
        assertNull(result.get(0).getId());

        // 10:00 -> persisted BOOKED
        assertEquals(SlotStatus.BOOKED, result.get(1).getStatus());
        assertEquals(bookedSlotId, result.get(1).getId());

        // 11:00 -> default unavailable
        assertEquals(SlotStatus.THERAPIST_UNAVAILABLE, result.get(2).getStatus());
        assertNull(result.get(2).getId());
    }

    @Test
    @DisplayName("Should throw InvalidSlotTimeException if slot time is not at top of hour")
    void testSaveSlotsInvalidMinute() {
        Long start = toEpochMilli(2026, 9, 27, 9, 0);
        Long end = toEpochMilli(2026, 9, 27, 18, 0);

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(mockTherapist));

        List<SlotRequestDto> requests = List.of(
                new SlotRequestDto(null, toEpochMilli(2026, 9, 27, 10, 30), SlotStatus.THERAPIST_AVAILABLE)
        );

        assertThrows(InvalidSlotTimeException.class, () ->
                slotService.saveSlots(therapistId, start, end, requests));
    }

    @Test
    @DisplayName("Should throw SlotConflictException when trying to modify already BOOKED slot")
    void testSaveSlotsConflictWithBookedSlot() {
        Long start = toEpochMilli(2026, 9, 27, 9, 0);
        Long end = toEpochMilli(2026, 9, 27, 18, 0);
        LocalDateTime startDT = LocalDateTime.of(2026, 9, 27, 9, 0);
        LocalDateTime endDT = LocalDateTime.of(2026, 9, 27, 18, 0);
        LocalDateTime slotTimeDT = LocalDateTime.of(2026, 9, 27, 10, 0);

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(mockTherapist));

        Slot bookedSlot = Slot.builder()
                .id(UUID.randomUUID())
                .therapist(mockTherapist)
                .slotTime(slotTimeDT)
                .status(SlotStatus.BOOKED)
                .build();

        when(slotRepository.findByTherapistIdAndSlotTimeBetween(therapistId, startDT, endDT))
                .thenReturn(List.of(bookedSlot));

        List<SlotRequestDto> requests = List.of(
                new SlotRequestDto(null, toEpochMilli(2026, 9, 27, 10, 0), SlotStatus.THERAPIST_AVAILABLE)
        );

        assertThrows(SlotConflictException.class, () ->
                slotService.saveSlots(therapistId, start, end, requests));
    }

    @Test
    @DisplayName("Should successfully save availability and persist slots")
    void testSaveSlotsSuccess() {
        Long start = toEpochMilli(2026, 9, 27, 9, 0);
        Long end = toEpochMilli(2026, 9, 27, 11, 0);
        LocalDateTime startDT = LocalDateTime.of(2026, 9, 27, 9, 0);
        LocalDateTime endDT = LocalDateTime.of(2026, 9, 27, 11, 0);
        LocalDateTime slotTimeDT = LocalDateTime.of(2026, 9, 27, 9, 0);

        when(therapistRepository.findById(therapistId)).thenReturn(Optional.of(mockTherapist));
        when(therapistRepository.existsById(therapistId)).thenReturn(true);
        when(slotRepository.findByTherapistIdAndSlotTimeBetween(therapistId, startDT, endDT))
                .thenReturn(List.of());

        List<SlotRequestDto> requests = List.of(
                new SlotRequestDto(null, toEpochMilli(2026, 9, 27, 9, 0), SlotStatus.THERAPIST_AVAILABLE)
        );

        slotService.saveSlots(therapistId, start, end, requests);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(captor.capture());

        List<Slot> savedSlots = captor.getValue();
        assertEquals(1, savedSlots.size());
        assertEquals(slotTimeDT, savedSlots.getFirst().getSlotTime());
        assertEquals(SlotStatus.THERAPIST_AVAILABLE, savedSlots.getFirst().getStatus());
    }
}
