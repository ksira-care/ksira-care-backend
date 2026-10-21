package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.AdminProperties;
import com.ksiracare.backend.exception.HistoryMonthNotAvailableException;
import com.ksiracare.backend.time.PortalCalendar;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Which months History covers: the current calendar month and the ones before it, up to
 * {@code historyMonths} in all (portal time zone). The export is always the month before the
 * current one; anything older than the window is due for deletion once it has been exported.
 */
@Component
public class HistoryWindow {

    private final PortalCalendar calendar;
    private final int months;

    public HistoryWindow(PortalCalendar calendar, AdminProperties properties) {
        this.calendar = calendar;
        this.months = properties.historyMonths();
    }

    /** Newest first, e.g. [2026-10, 2026-09]. */
    public List<YearMonth> months() {
        YearMonth current = calendar.currentMonth();
        return IntStream.range(0, months).mapToObj(current::minusMonths).toList();
    }

    /** The month the CSV export covers: the one before the current month. */
    public YearMonth exportMonth() {
        return calendar.currentMonth().minusMonths(1);
    }

    /** The oldest month History still shows; bookings before it are past the window. */
    public YearMonth oldestMonth() {
        return calendar.currentMonth().minusMonths(months - 1L);
    }

    public void checkShown(YearMonth month) {
        if (!months().contains(month)) {
            throw new HistoryMonthNotAvailableException(
                    "History covers " + months().getLast() + " to " + months().getFirst() + ".");
        }
    }
}
