package com.ksiracare.backend.service;

import com.ksiracare.backend.config.PublicBookingProperties;
import com.ksiracare.backend.dto.request.PublicBookingRequestDto;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.policy.FixedIntervalSlotSchedulePolicy;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.LanguageRepository;
import com.ksiracare.backend.security.PublicBookingLimiter;
import com.ksiracare.backend.support.TestClocks;
import com.ksiracare.backend.time.EpochTime;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.ksiracare.backend.support.TestClocks.utc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The two-hour lead time, at a fixed moment: 14:00 IST. */
class PublicBookingServiceTest {

    private final PortalCalendar calendar = TestClocks.calendarAt("2026-10-21T14:00");
    private final HourCapacity capacity = mock(HourCapacity.class);
    private final PublicBookingProperties properties = new PublicBookingProperties(true, Duration.ofHours(2), 5, Duration.ofHours(1));
    private final PublicBookingService service = new PublicBookingService(
            mock(BookingRepository.class), mock(LanguageRepository.class), capacity,
            new FixedIntervalSlotSchedulePolicy(calendar, TestClocks.PORTAL), calendar, TestClocks.PORTAL, properties,
            new PublicBookingLimiter(properties, TestClocks.atIst("2026-10-21T14:00")));

    @Test
    void theFirstHourOfferedIsTwoHoursAway() {
        when(capacity.hoursWithRoomFor(any(), any())).thenReturn(List.of());
        ArgumentCaptor<UtcRange> range = ArgumentCaptor.forClass(UtcRange.class);

        service.getBookableHours(new UtcRange(utc("2026-10-21T00:00"), utc("2026-10-22T00:00")), Set.of(Language.HINDI));

        verify(capacity).hoursWithRoomFor(range.capture(), eq(Set.of(Language.HINDI)));
        assertThat(range.getValue().start()).isEqualTo(utc("2026-10-21T16:00"));
    }

    @Test
    void anHourSoonerThanThatCannotBeBooked() {
        assertThatThrownBy(() -> service.book(request("2026-10-21T15:00"), "203.0.113.1"))
                .isInstanceOf(InvalidSlotTimeException.class)
                .hasMessageContaining("2 hours");
    }

    private static PublicBookingRequestDto request(String istStart) {
        return new PublicBookingRequestDto("Tom H", "tom@example.com", EpochTime.toMillis(utc(istStart)),
                Set.of(Language.HINDI), null, true);
    }
}
