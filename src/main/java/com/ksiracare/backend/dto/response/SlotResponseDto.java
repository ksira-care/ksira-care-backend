package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.SlotStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponseDto {

    private UUID id;

    private Long time; // Epoch timestamp in milliseconds (UTC)

    private SlotStatus status;
}
