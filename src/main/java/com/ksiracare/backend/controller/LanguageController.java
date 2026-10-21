package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.LanguagesResponseDto;
import com.ksiracare.backend.service.LanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The languages aaptas can speak, for the language pickers of both the portal and the admin console. */
@RestController
@RequestMapping("/languages")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    public LanguagesResponseDto getLanguages() {
        return new LanguagesResponseDto(languageService.getLanguages());
    }
}
