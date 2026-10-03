package com.ksiracare.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/** Languages clients and therapists can be matched on. Sent to clients as ISO 639-1 codes. */
public enum Language {
    ENGLISH("en"),
    HINDI("hi"),
    MARATHI("mr"),
    KANNADA("kn");

    private final String isoCode;

    Language(String isoCode) {
        this.isoCode = isoCode;
    }

    @JsonValue
    public String isoCode() {
        return isoCode;
    }
}
