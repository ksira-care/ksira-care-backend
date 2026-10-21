package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.PortalProperties;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Back-to-back sessions of {@code sessionLength}, starting at {@code firstSlotHour} and no later
 * than {@code lastSlotHour} in the portal time zone (hourly, 09:00–23:00 IST by default), up to
 * {@code bookingWindowDays} ahead.
 */
@Component
public class FixedIntervalSlotSchedulePolicy implements SlotSchedulePolicy {

    private final PortalCalendar calendar;
    private final LocalTime firstStart;
    private final LocalTime lastStart;
    private final Duration sessionLength;
    private final int windowDays;

    public FixedIntervalSlotSchedulePolicy(PortalCalendar calendar, PortalProperties properties) {
        this.calendar = calendar;
        this.firstStart = LocalTime.of(properties.firstSlotHour(), 0);
        this.lastStart = LocalTime.of(properties.lastSlotHour(), 0);
        this.sessionLength = properties.sessionLength();
        this.windowDays = properties.bookingWindowDays();
    }

    @Override
    public List<LocalDateTime> slotStartsWithin(UtcRange range) {
        List<LocalDateTime> starts = new ArrayList<>();
        LocalDate lastDay = calendar.dateOf(range.end());
        for (LocalDate day = calendar.dateOf(range.start()); !day.isAfter(lastDay); day = day.plusDays(1)) {
            for (LocalDateTime local = day.atTime(firstStart); !local.isAfter(day.atTime(lastStart)); local = local.plus(sessionLength)) {
                LocalDateTime start = calendar.toUtc(day, local.toLocalTime());
                if (range.contains(start)) {
                    starts.add(start);
                }
            }
        }
        return starts;
    }

    @Override
    public boolean isSlotStart(LocalDateTime slotStartUtc) {
        LocalTime local = calendar.timeOf(slotStartUtc);
        if (local.isBefore(firstStart) || local.isAfter(lastStart)) {
            return false;
        }
        long sinceFirst = Duration.between(firstStart, local).toNanos();
        return sinceFirst % sessionLength.toNanos() == 0;
    }

    @Override
    public LocalDateTime windowEnd() {
        return calendar.dayOf(calendar.today().plusDays(windowDays)).end();
    }

    @Override
    public boolean isOpenForChanges(LocalDateTime slotStartUtc, LocalDateTime nowUtc) {
        boolean notStarted = slotStartUtc.isAfter(nowUtc);
        boolean withinWindow = !calendar.dateOf(slotStartUtc).isAfter(calendar.today().plusDays(windowDays));
        return notStarted && withinWindow;
    }
}
