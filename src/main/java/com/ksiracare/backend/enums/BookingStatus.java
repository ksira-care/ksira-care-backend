package com.ksiracare.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BookingStatus {
    PENDING,
    COMPLETED,
    CLIENT_NO_SHOW,
    THERAPIST_NO_SHOW,
    CANCELLED;

    @JsonCreator
    public static BookingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return BookingStatus.valueOf(normalized);
    }
}
