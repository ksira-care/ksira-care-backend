package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.enums.SlotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotRequestDto {

    private UUID id;

    @NotNull(message = "Slot time is required")
    private Long time; // Epoch timestamp in milliseconds (or seconds)

    @NotNull(message = "Slot status is required")
    private SlotStatus status;
}
