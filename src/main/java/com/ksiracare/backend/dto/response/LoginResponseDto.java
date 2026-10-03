package com.ksiracare.backend.dto.response;

import java.util.UUID;

/** The session itself travels in the httpOnly cookie, never in the body. */
public record LoginResponseDto(UUID therapistId, String email) {
}
