package com.ksiracare.backend.dto.response;

import java.util.List;

/** The roster: every aapta who can sign in, by name. */
public record AaptasResponseDto(List<AaptaSummaryDto> aaptas) {
}
