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

    private final BookingRepository bookingRepository;
    private final PortalCalendar calendar;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(UUID therapistId) {
        UtcRange thisMonth = calendar.monthOf(calendar.today());
        long completedThisMonth = bookingRepository.countWithStatusBetween(
                therapistId, BookingStatus.COMPLETED, thisMonth.start(), thisMonth.end());
        long completedAllTime = bookingRepository.countWithStatus(therapistId, BookingStatus.COMPLETED);
        LocalDate activeSince = bookingRepository.findEarliestStartWithStatus(therapistId, BookingStatus.COMPLETED)
                .map(calendar::dateOf)
                .orElse(null);
        return new DashboardSummaryDto(completedThisMonth, completedAllTime, activeSince);
    }
}
