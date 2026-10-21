package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.enums.SlotStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * One hour the aapta is opening or closing.
 *
 * @param time start of the hour, epoch milliseconds (UTC)
 */
public record SlotRequestDto(
        UUID id,
        @NotNull(message = "Slot time is required") Long time,
        @NotNull(message = "Slot status is required") SlotStatus status
) {
}
