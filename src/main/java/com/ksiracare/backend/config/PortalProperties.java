package com.ksiracare.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Calendar rules for the aapta portal ({@code ksira.portal.*}).
 *
 * @param timeZone          zone in which days, months and opening hours are defined (times are stored in UTC)
 * @param bookingWindowDays how many days ahead aaptas may open hours
 * @param firstSlotHour     first session start of the day, in {@code timeZone}
 * @param lastSlotHour      last session start of the day, in {@code timeZone}
 * @param sessionLength     length of one session; sessions start back to back from {@code firstSlotHour}
 * @param maxQueryRange     longest [startTime, endTime) range a client may request in one call
 */
@Validated
@ConfigurationProperties("ksira.portal")
public record PortalProperties(
        @NotNull ZoneId timeZone,
        @Min(1) int bookingWindowDays,
        @Min(0) @Max(23) int firstSlotHour,
        @Min(0) @Max(23) int lastSlotHour,
        @NotNull @DurationMin(minutes = 15) @DurationMax(hours = 4) Duration sessionLength,
        @NotNull @DurationMin(days = 1) Duration maxQueryRange
) {
}
