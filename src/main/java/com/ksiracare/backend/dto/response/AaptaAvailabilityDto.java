package com.ksiracare.backend.dto.response;

import java.util.List;
import java.util.UUID;

/** One row of the availability grid: an aapta and their status at each hour of the day. */
public record AaptaAvailabilityDto(UUID aaptaId, String firstName, String lastName, List<AvailabilityCellDto> slots) {
}
