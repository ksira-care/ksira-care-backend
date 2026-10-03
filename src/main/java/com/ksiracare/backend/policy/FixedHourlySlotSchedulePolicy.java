package com.ksiracare.backend.policy;

import com.ksiracare.backend.config.PortalProperties;
import com.ksiracare.backend.time.PortalCalendar;
import com.ksiracare.backend.time.UtcRange;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One-hour sessions starting on the hour, from {@code firstSlotHour} to {@code lastSlotHour}
 * in the portal time zone (09:00–23:00 IST by default), up to {@code bookingWindowDays} ahead.
 */
@Component
public class FixedHourlySlotSchedulePolicy implements SlotSchedulePolicy {

    private final PortalCalendar calendar;
    private final int firstHour;
    private final int lastHour;
    private final int windowDays;

    public FixedHourlySlotSchedulePolicy(PortalCalendar calendar, PortalProperties properties) {
        this.calendar = calendar;
        this.firstHour = properties.firstSlotHour();
        this.lastHour = properties.lastSlotHour();
        this.windowDays = properties.bookingWindowDays();
    }

    @Override
    public List<LocalDateTime> slotStartsWithin(UtcRange range) {
        List<LocalDateTime> starts = new ArrayList<>();
        LocalDate lastDay = calendar.dateOf(range.end());
        for (LocalDate day = calendar.dateOf(range.start()); !day.isAfter(lastDay); day = day.plusDays(1)) {
            for (int hour = firstHour; hour <= lastHour; hour++) {
                LocalDateTime start = calendar.toUtc(day, LocalTime.of(hour, 0));
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
        boolean onTheHour = local.getMinute() == 0 && local.getSecond() == 0 && local.getNano() == 0;
        return onTheHour && local.getHour() >= firstHour && local.getHour() <= lastHour;
    }

    @Override
    public boolean isOpenForChanges(LocalDateTime slotStartUtc, LocalDateTime nowUtc) {
        boolean notStarted = slotStartUtc.isAfter(nowUtc);
        boolean withinWindow = !calendar.dateOf(slotStartUtc).isAfter(calendar.today().plusDays(windowDays));
        return notStarted && withinWindow;
    }
}
