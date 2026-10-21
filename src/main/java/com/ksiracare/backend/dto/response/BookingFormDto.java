package com.ksiracare.backend.dto.response;

import java.util.UUID;

/**
 * What the client entered on the website's booking form ("View form"), field for field.
 *
 * @param notes       what they'd like to talk about, if anything
 * @param consent     whether they accepted the terms when booking
 * @param submittedAt epoch milliseconds (UTC)
 */
public record BookingFormDto(
        UUID bookingId,
        String name,
        String email,
        String notes,
        boolean consent,
        Long submittedAt
) {
}
