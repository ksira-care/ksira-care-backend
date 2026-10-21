package com.ksiracare.backend.service;

import com.ksiracare.backend.policy.HistoryWindow;
import com.ksiracare.backend.repository.BookingRepository;
import com.ksiracare.backend.repository.HistoryExportRepository;
import com.ksiracare.backend.repository.SlotRepository;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Bookings are kept for History's window (this month and last). Older months are deleted — with
 * their history, client languages and the aaptas' hours — but only once they have been exported;
 * a month nobody exported is kept, and History warns about it, so nothing is lost unseen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryRetention {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final HistoryExportRepository exportRepository;
    private final HistoryWindow window;
    private final PortalCalendar calendar;

    /**
     * @param deleted     months whose bookings were deleted
     * @param notExported older months still kept because they were never exported
     */
    public record Outcome(List<YearMonth> deleted, List<YearMonth> notExported) {
    }

    /** Older months that still hold bookings but were never exported, oldest first. */
    @Transactional(readOnly = true)
    public List<YearMonth> notExportedMonths() {
        Set<LocalDate> exported = exportRepository.findExportedMonths();
        return monthsBeyondWindow().stream().filter(month -> !exported.contains(month.atDay(1))).toList();
    }

    /** Deletes every month beyond the window that has been exported. */
    @Transactional
    public Outcome deleteExportedMonths() {
        Set<LocalDate> exported = exportRepository.findExportedMonths();
        List<YearMonth> deleted = new ArrayList<>();
        List<YearMonth> notExported = new ArrayList<>();
        for (YearMonth month : monthsBeyondWindow()) {
            if (exported.contains(month.atDay(1))) {
                delete(month);
                deleted.add(month);
            } else {
                notExported.add(month);
            }
        }
        return new Outcome(deleted, notExported);
    }

    /** Months before the window that still hold bookings, oldest first. */
    private List<YearMonth> monthsBeyondWindow() {
        YearMonth oldestKept = window.oldestMonth();
        List<YearMonth> months = new ArrayList<>();
        bookingRepository.findEarliestStart().map(start -> YearMonth.from(calendar.dateOf(start))).ifPresent(earliest -> {
            for (YearMonth month = earliest; month.isBefore(oldestKept); month = month.plusMonths(1)) {
                UtcRange range = calendar.monthOf(month);
                if (bookingRepository.countStartingBetween(range.start(), range.end()) > 0) {
                    months.add(month);
                }
            }
        });
        return months;
    }

    private void delete(YearMonth month) {
        UtcRange range = calendar.monthOf(month);
        bookingRepository.deleteEventsOfBookingsStartingBetween(range.start(), range.end());
        bookingRepository.deleteLanguagesOfBookingsStartingBetween(range.start(), range.end());
        int bookings = bookingRepository.deleteStartingBetween(range.start(), range.end());
        slotRepository.deleteUnbookedStartingBetween(range.start(), range.end());
        log.info("Deleted {} booking(s) from {} (exported earlier)", bookings, month);
    }
}
