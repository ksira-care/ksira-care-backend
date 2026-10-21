package com.ksiracare.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SlotStatus {
    BOOKED,
    AAPTA_AVAILABLE,
    AAPTA_UNAVAILABLE;

    @JsonCreator
    public static SlotStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("AAPTA_UNVAILABLE".equals(normalized)) {
            return AAPTA_UNAVAILABLE;
        }
        return SlotStatus.valueOf(normalized);
    }
}
