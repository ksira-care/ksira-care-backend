package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.ApiException;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.exception.SlotConflictException;
import com.ksiracare.backend.mapper.SlotMapper;
import com.ksiracare.backend.mapper.SlotMapperImpl;
import com.ksiracare.backend.policy.FixedIntervalSlotSchedulePolicy;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.repository.AaptaRepository;
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

class SlotServiceTest {

    private static final UUID AAPTA_ID = UUID.randomUUID();
    private static final UtcRange OCT_5 = new UtcRange(utc("2026-10-05T00:00"), utc("2026-10-06T00:00"));

    private final SlotRepository slotRepository = mock(SlotRepository.class);
    private final AaptaRepository aaptaRepository = mock(AaptaRepository.class);
    private final PortalCalendar calendar = TestClocks.calendarAt("2026-10-03T15:00");
    private final SlotMapper mapper = new SlotMapperImpl();
    private SlotService service;
    private final Aapta aapta = new Aapta("Aanya", "Mehta", "a@k.com", "hash");

    @BeforeEach
    void setUp() {
        service = new SlotService(aaptaRepository, slotRepository,
                new FixedIntervalSlotSchedulePolicy(calendar, TestClocks.PORTAL), mapper, calendar);
        when(aaptaRepository.getReferenceById(AAPTA_ID)).thenReturn(aapta);
        when(slotRepository.findForAapta(eq(AAPTA_ID), any(), any())).thenReturn(List.of());
    }

    private static SlotRequestDto change(String istTime, SlotStatus status) {
        return new SlotRequestDto(null, EpochTime.toMillis(utc(istTime)), status);
    }

    @Test
    void reportsEveryHourAndTreatsUnrecordedOnesAsClosed() {
        when(slotRepository.findForAapta(eq(AAPTA_ID), any(), any()))
                .thenReturn(List.of(new Slot(aapta, utc("2026-10-05T10:00"), SlotStatus.AAPTA_AVAILABLE)));

        List<SlotResponseDto> slots = service.getSlots(AAPTA_ID, OCT_5);

        assertThat(slots).hasSize(15);
        assertThat(slots.get(0).status()).isEqualTo(SlotStatus.AAPTA_UNAVAILABLE);
        assertThat(slots.get(1).status()).isEqualTo(SlotStatus.AAPTA_AVAILABLE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void savesOnlyTheHoursSent() {
        service.saveSlots(AAPTA_ID, OCT_5, List.of(change("2026-10-05T09:00", SlotStatus.AAPTA_AVAILABLE)));

        ArgumentCaptor<List<Slot>> saved = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement()
                .satisfies(slot -> assertThat(slot.getSlotTime()).isEqualTo(utc("2026-10-05T09:00")));
    }

    @Test
    void refusesToChangeABookedHour() {
        when(slotRepository.findForAapta(eq(AAPTA_ID), any(), any()))
                .thenReturn(List.of(new Slot(aapta, utc("2026-10-05T09:00"), SlotStatus.BOOKED)));

        assertThatThrownBy(() -> service.saveSlots(AAPTA_ID, OCT_5,
                List.of(change("2026-10-05T09:00", SlotStatus.AAPTA_UNAVAILABLE))))
                .isInstanceOf(SlotConflictException.class);
        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void refusesToSetBooked() {
        assertThatThrownBy(() -> service.saveSlots(AAPTA_ID, OCT_5, List.of(change("2026-10-05T09:00", SlotStatus.BOOKED))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("opened or closed");
    }

    @Test
    void refusesHoursOutsideOpeningTimesOrAlreadyStarted() {
        assertThatThrownBy(() -> service.saveSlots(AAPTA_ID, OCT_5, List.of(change("2026-10-05T08:00", SlotStatus.AAPTA_AVAILABLE))))
                .isInstanceOf(InvalidSlotTimeException.class);

        UtcRange today = new UtcRange(utc("2026-10-03T00:00"), utc("2026-10-04T00:00"));
        assertThatThrownBy(() -> service.saveSlots(AAPTA_ID, today, List.of(change("2026-10-03T14:00", SlotStatus.AAPTA_AVAILABLE))))
                .isInstanceOf(InvalidSlotTimeException.class);
    }

    @Test
    void refusesTheSameHourTwice() {
        assertThatThrownBy(() -> service.saveSlots(AAPTA_ID, OCT_5, List.of(
                change("2026-10-05T09:00", SlotStatus.AAPTA_AVAILABLE),
                change("2026-10-05T09:00", SlotStatus.AAPTA_UNAVAILABLE))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("twice");
    }
}
