package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.Language;

/** A language aaptas and clients can be matched on, e.g. {@code {"code": "hi", "name": "Hindi"}}. */
public record LanguageDto(Language code, String name) {
}
