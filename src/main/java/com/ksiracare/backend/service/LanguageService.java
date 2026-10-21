package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.response.LanguageDto;
import com.ksiracare.backend.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final LanguageRepository languageRepository;

    /** The languages aaptas can speak, by name. */
    @Transactional(readOnly = true)
    public List<LanguageDto> getLanguages() {
        return languageRepository.findAllByOrderByNameAsc().stream()
                .map(language -> new LanguageDto(language.getCode(), language.getName()))
                .toList();
    }
}
