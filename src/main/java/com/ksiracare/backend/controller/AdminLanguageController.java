package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.LanguagesResponseDto;
import com.ksiracare.backend.service.AdminAaptaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The languages aaptas can be given, for the admin console's pickers. */
@RestController
@RequestMapping("/admin/languages")
@RequiredArgsConstructor
public class AdminLanguageController {

    private final AdminAaptaService adminAaptaService;

    @GetMapping
    public LanguagesResponseDto getLanguages() {
        return new LanguagesResponseDto(adminAaptaService.getLanguages());
    }
}
