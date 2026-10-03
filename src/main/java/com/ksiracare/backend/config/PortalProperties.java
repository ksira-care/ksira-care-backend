package com.ksiracare.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Calendar rules for the therapist portal ({@code ksira.portal.*}).
 *
 * @param timeZone           zone in which days, months and opening hours are defined (times are stored in UTC)
 * @param bookingWindowDays  how many days ahead therapists may open hours
 * @param firstSlotHour      first session start of the day, in {@code timeZone}
 * @param lastSlotHour       last session start of the day, in {@code timeZone}
 */
@Validated
@ConfigurationProperties("ksira.portal")
public record PortalProperties(
        @NotNull ZoneId timeZone,
        @Min(1) int bookingWindowDays,
        @Min(0) @Max(23) int firstSlotHour,
        @Min(0) @Max(23) int lastSlotHour
) {
}
