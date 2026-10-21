package com.ksiracare.backend.dto.response;

import java.util.UUID;

/** Just enough to name an aapta in the admin console, e.g. "Aanya M.". */
public record AaptaRefDto(UUID aaptaId, String firstName, String lastName) {
}
