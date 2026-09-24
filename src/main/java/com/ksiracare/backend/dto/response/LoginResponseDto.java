package com.ksiracare.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@Builder
public class LoginResponseDto {
    private String token;
    private UUID therapistId;
    private String email;
}