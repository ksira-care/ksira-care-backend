package com.ksiracare.backend.policy;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotSchedulePolicy {

    /**
     * Generates all fixed slot time instances between startTime and endTime (inclusive).
     *
     * @param startTime lower time bound
     * @param endTime   upper time bound
     * @return chronologically sorted list of valid fixed slot times
     */
    List<LocalDateTime> generateFixedSlotTimes(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Validates whether a given time conforms to the fixed slot schedule.
     *
     * @param slotTime slot time candidate
     * @return true if valid, false otherwise
     */
    boolean isValidSlotTime(LocalDateTime slotTime);
}
