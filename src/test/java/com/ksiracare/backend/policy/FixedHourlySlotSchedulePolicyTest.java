package com.ksiracare.backend.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FixedHourlySlotSchedulePolicyTest {

    private FixedHourlySlotSchedulePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new FixedHourlySlotSchedulePolicy();
    }

    @Test
    @DisplayName("Should generate fixed slots between 9 AM and 11 PM for a given day")
    void testGenerateFixedSlotTimesSingleDay() {
        LocalDate today = LocalDate.of(2026, 9, 27);
        LocalDateTime start = LocalDateTime.of(today, LocalTime.of(0, 0));
        LocalDateTime end = LocalDateTime.of(today, LocalTime.of(23, 59));

        List<LocalDateTime> slots = policy.generateFixedSlotTimes(start, end);

        // From 9 AM to 11 PM inclusive is 15 slots (9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)
        assertEquals(15, slots.size());
        assertEquals(LocalDateTime.of(today, LocalTime.of(9, 0)), slots.getFirst());
        assertEquals(LocalDateTime.of(today, LocalTime.of(23, 0)), slots.getLast());
    }

    @Test
    @DisplayName("Should respect narrow start and end boundaries")
    void testGenerateFixedSlotTimesBounded() {
        LocalDate today = LocalDate.of(2026, 9, 27);
        LocalDateTime start = LocalDateTime.of(today, LocalTime.of(10, 30));
        LocalDateTime end = LocalDateTime.of(today, LocalTime.of(14, 0));

        List<LocalDateTime> slots = policy.generateFixedSlotTimes(start, end);

        // 11:00, 12:00, 13:00, 14:00 (10:00 is before 10:30 so excluded)
        assertEquals(4, slots.size());
        assertEquals(LocalDateTime.of(today, LocalTime.of(11, 0)), slots.get(0));
        assertEquals(LocalDateTime.of(today, LocalTime.of(14, 0)), slots.get(3));
    }

    @Test
    @DisplayName("Should validate hourly slots correctly")
    void testIsValidSlotTime() {
        LocalDate today = LocalDate.of(2026, 9, 27);

        // Valid slots
        assertTrue(policy.isValidSlotTime(LocalDateTime.of(today, LocalTime.of(9, 0))));
        assertTrue(policy.isValidSlotTime(LocalDateTime.of(today, LocalTime.of(14, 0))));
        assertTrue(policy.isValidSlotTime(LocalDateTime.of(today, LocalTime.of(23, 0))));

        // Invalid: minutes != 0
        assertFalse(policy.isValidSlotTime(LocalDateTime.of(today, LocalTime.of(9, 30))));
        // Invalid: before 9 AM
        assertFalse(policy.isValidSlotTime(LocalDateTime.of(today, LocalTime.of(8, 0))));
        // Invalid: after 11 PM
        assertFalse(policy.isValidSlotTime(LocalDateTime.of(today, LocalTime.of(0, 0))));
        // Invalid: null
        assertFalse(policy.isValidSlotTime(null));
    }
}
