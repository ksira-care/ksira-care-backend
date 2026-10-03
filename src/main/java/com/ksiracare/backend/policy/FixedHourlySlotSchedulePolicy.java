package com.ksiracare.backend.policy;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class FixedHourlySlotSchedulePolicy implements SlotSchedulePolicy {

    public static final int START_HOUR = 9;   // 9:00 AM
    public static final int END_HOUR = 23;    // 11:00 PM

    @Override
    public List<LocalDateTime> generateFixedSlotTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            return List.of();
        }

        List<LocalDateTime> slotTimes = new ArrayList<>();
        LocalDate currentDate = startTime.toLocalDate();
        LocalDate lastDate = endTime.toLocalDate();

        while (!currentDate.isAfter(lastDate)) {
            for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
                LocalDateTime candidate = LocalDateTime.of(currentDate, LocalTime.of(hour, 0, 0));
                if (!candidate.isBefore(startTime) && !candidate.isAfter(endTime)) {
                    slotTimes.add(candidate);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return slotTimes;
    }

    @Override
    public boolean isValidSlotTime(LocalDateTime slotTime) {
        if (slotTime == null) {
            return false;
        }

        boolean isHourly = slotTime.getMinute() == 0 && slotTime.getSecond() == 0 && slotTime.getNano() == 0;
        boolean isWithinOperatingHours = slotTime.getHour() >= START_HOUR && slotTime.getHour() <= END_HOUR;

        return isHourly && isWithinOperatingHours;
    }
}
