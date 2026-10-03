package com.ksiracare.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SlotStatus {
    BOOKED,
    THERAPIST_AVAILABLE,
    THERAPIST_UNAVAILABLE;

    @JsonCreator
    public static SlotStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("THERAPIST_UNVAILABLE".equals(normalized)) {
            return THERAPIST_UNAVAILABLE;
        }
        return SlotStatus.valueOf(normalized);
    }
}
