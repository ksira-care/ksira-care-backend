package com.ksiracare.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/** Languages clients and aaptas can be matched on. Sent to clients as ISO 639-1 codes. */
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

    /** "hi" → HINDI. */
    public static Language fromIsoCode(String isoCode) {
        for (Language language : values()) {
            if (language.isoCode.equalsIgnoreCase(isoCode.trim())) {
                return language;
            }
        }
        throw new IllegalArgumentException("Unknown language code: " + isoCode);
    }
}
