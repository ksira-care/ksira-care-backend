package com.ksiracare.backend.dto.response;

import java.util.List;

/** Aaptas a booking can be given to: the current one (if any) first, then those who opened that hour. */
public record AaptaOptionsResponseDto(List<AaptaRefDto> aaptas) {
}
