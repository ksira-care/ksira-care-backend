package com.ksiracare.backend.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Admin console rules ({@code ksira.admin.*}).
 *
 * @param bookingActionWindow how long before a session admins may allocate it or change its
 *                            BCC flag; earlier than that, and once it has started, it is view-only
 * @param historyMonths       how many calendar months History shows, counting the current one
 *                            (2 = this month and last); older bookings are deleted once exported
 */
@Validated
@ConfigurationProperties("ksira.admin")
public record AdminProperties(
        @NotNull @DurationMin(hours = 1) Duration bookingActionWindow,
        @Min(2) int historyMonths
) {
}
