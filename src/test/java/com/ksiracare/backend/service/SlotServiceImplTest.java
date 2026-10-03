package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.ApiException;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.exception.SlotConflictException;
import com.ksiracare.backend.mapper.SlotMapper;
import com.ksiracare.backend.mapper.SlotMapperImpl;
import com.ksiracare.backend.policy.FixedHourlySlotSchedulePolicy;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.repository.TherapistRepository;
import com.ksiracare.backend.support.TestClocks;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlotServiceImplTest {

    private static final UUID THERAPIST_ID = UUID.randomUUID();
    private static final UtcRange OCT_5 = new UtcRange(utc("2026-10-05T00:00"), utc("2026-10-06T00:00"));

    private final SlotRepository slotRepository = mock(SlotRepository.class);
    private final TherapistRepository therapistRepository = mock(TherapistRepository.class);
    private final PortalCalendar calendar = TestClocks.calendarAt("2026-10-03T15:00");
    private final SlotMapper mapper = new SlotMapperImpl();
    private SlotServiceImpl service;
    private final Therapist therapist = new Therapist("Aanya", "Mehta", "a@k.com", "hash");

    @BeforeEach
    void setUp() {
        service = new SlotServiceImpl(therapistRepository, slotRepository,
                new FixedHourlySlotSchedulePolicy(calendar, TestClocks.PORTAL), mapper, calendar);
        when(therapistRepository.getReferenceById(THERAPIST_ID)).thenReturn(therapist);
        when(slotRepository.findForTherapist(eq(THERAPIST_ID), any(), any())).thenReturn(List.of());
    }

    private static SlotRequestDto change(String istTime, SlotStatus status) {
        return new SlotRequestDto(null, EpochTime.toMillis(utc(istTime)), status);
    }

    @Test
    void reportsEveryHourAndTreatsUnrecordedOnesAsClosed() {
        when(slotRepository.findForTherapist(eq(THERAPIST_ID), any(), any()))
                .thenReturn(List.of(new Slot(therapist, utc("2026-10-05T10:00"), SlotStatus.THERAPIST_AVAILABLE)));

        List<SlotResponseDto> slots = service.getSlots(THERAPIST_ID, OCT_5);

        assertThat(slots).hasSize(15);
        assertThat(slots.get(0).getStatus()).isEqualTo(SlotStatus.THERAPIST_UNAVAILABLE);
        assertThat(slots.get(1).getStatus()).isEqualTo(SlotStatus.THERAPIST_AVAILABLE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void savesOnlyTheHoursSent() {
        service.saveSlots(THERAPIST_ID, OCT_5, List.of(change("2026-10-05T09:00", SlotStatus.THERAPIST_AVAILABLE)));

        ArgumentCaptor<List<Slot>> saved = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement()
                .satisfies(slot -> assertThat(slot.getSlotTime()).isEqualTo(utc("2026-10-05T09:00")));
    }

    @Test
    void refusesToChangeABookedHour() {
        when(slotRepository.findForTherapist(eq(THERAPIST_ID), any(), any()))
                .thenReturn(List.of(new Slot(therapist, utc("2026-10-05T09:00"), SlotStatus.BOOKED)));

        assertThatThrownBy(() -> service.saveSlots(THERAPIST_ID, OCT_5,
                List.of(change("2026-10-05T09:00", SlotStatus.THERAPIST_UNAVAILABLE))))
                .isInstanceOf(SlotConflictException.class);
        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void refusesToSetBooked() {
        assertThatThrownBy(() -> service.saveSlots(THERAPIST_ID, OCT_5, List.of(change("2026-10-05T09:00", SlotStatus.BOOKED))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("opened or closed");
    }

    @Test
    void refusesHoursOutsideOpeningTimesOrAlreadyStarted() {
        assertThatThrownBy(() -> service.saveSlots(THERAPIST_ID, OCT_5, List.of(change("2026-10-05T08:00", SlotStatus.THERAPIST_AVAILABLE))))
                .isInstanceOf(InvalidSlotTimeException.class);

        UtcRange today = new UtcRange(utc("2026-10-03T00:00"), utc("2026-10-04T00:00"));
        assertThatThrownBy(() -> service.saveSlots(THERAPIST_ID, today, List.of(change("2026-10-03T14:00", SlotStatus.THERAPIST_AVAILABLE))))
                .isInstanceOf(InvalidSlotTimeException.class);
    }

    @Test
    void refusesTheSameHourTwice() {
        assertThatThrownBy(() -> service.saveSlots(THERAPIST_ID, OCT_5, List.of(
                change("2026-10-05T09:00", SlotStatus.THERAPIST_AVAILABLE),
                change("2026-10-05T09:00", SlotStatus.THERAPIST_UNAVAILABLE))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("twice");
    }
}
