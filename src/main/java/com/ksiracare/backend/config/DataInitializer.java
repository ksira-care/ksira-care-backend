package com.ksiracare.backend.config;

import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LanguageRepository languageRepository;

    @Override
    public void run(String @NonNull ... args) {
        Arrays.stream(Language.values()).forEach(language -> {
            if (languageRepository.findByCode(language).isEmpty()) {
                languageRepository.save(new LanguageEntity(language, language.name()));
            }
        });
    }
}
