package com.ksiracare.backend.dto;

import com.ksiracare.backend.enums.Language;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class TherapistResponseDto {
    private Long id;
    private String fullName;
    private String displayName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private Set<Language> languages;
    private String address;
}
