package com.ksiracare.backend.dto.response;

import java.util.List;

/** The roster: every aapta who can sign in, by name, each in the profile format the aapta sees. */
public record AaptasResponseDto(List<AaptaProfileDto> aaptas) {
}
