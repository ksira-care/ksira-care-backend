package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.DashboardSummaryDto;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    /** The dashboard's "recent" window, matching what the aapta portal lists. */
    static final int RECENT_DAYS = 30;

    private final BookingRepository bookingRepository;
    private final PortalCalendar calendar;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(UUID aaptaId) {
        LocalDate today = calendar.today();
        UtcRange thisMonth = calendar.monthOf(today);
        UtcRange recent = new UtcRange(calendar.dayOf(today.minusDays(RECENT_DAYS)).start(), calendar.dayOf(today).end());
        return new DashboardSummaryDto(completed(aaptaId, thisMonth), completed(aaptaId, recent));
    }

    private long completed(UUID aaptaId, UtcRange range) {
        return bookingRepository.countWithStatusBetween(aaptaId, BookingStatus.COMPLETED, range.start(), range.end());
    }
}
