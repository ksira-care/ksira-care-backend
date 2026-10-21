package com.ksiracare.backend.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Booking from the public website ({@code ksira.public-booking.*}).
 *
 * @param enabled      whether {@code /public/**} answers at all; off in production until the site opens
 * @param leadTime     how soon a session may start: booking at 14:00 offers 16:00 onwards
 * @param maxPerClient bookings one client address may make per {@code rateWindow}
 * @param rateWindow   the window {@code maxPerClient} applies to
 */
@Validated
@ConfigurationProperties("ksira.public-booking")
public record PublicBookingProperties(
        boolean enabled,
        @NotNull Duration leadTime,
        @Min(1) int maxPerClient,
        @NotNull Duration rateWindow
) {
}
