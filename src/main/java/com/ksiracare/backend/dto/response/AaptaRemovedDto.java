package com.ksiracare.backend.dto.response;

import java.util.UUID;

/**
 * The result of removing an aapta.
 *
 * @param unallocatedBookings how many of their upcoming sessions went back to unallocated
 */
public record AaptaRemovedDto(UUID aaptaId, int unallocatedBookings) {
}
