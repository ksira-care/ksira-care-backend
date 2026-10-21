package com.ksiracare.backend.config;

import com.ksiracare.backend.enums.Language;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets query parameters name languages the way the API writes them ({@code ?languages=hi,en}).
 * An unknown code is a 400 VALIDATION_FAILED.
 */
@Component
public class LanguageCodeConverter implements Converter<String, Language> {

    @Override
    public Language convert(String isoCode) {
        return Language.fromIsoCode(isoCode);
    }
}
