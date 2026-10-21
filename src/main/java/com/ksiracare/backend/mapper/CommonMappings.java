package com.ksiracare.backend.mapper;

import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.time.EpochTime;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Conversions shared by every mapper ({@code @Mapper(uses = CommonMappings.class)}). MapStruct
 * applies them automatically wherever the source and target types match.
 */
public final class CommonMappings {

    private CommonMappings() {
    }

    /** Stored UTC → API epoch milliseconds. */
    public static Long toMillis(LocalDateTime utc) {
        return EpochTime.toMillis(utc);
    }

    public static Set<Language> toLanguages(Set<LanguageEntity> languages) {
        return languages == null ? Set.of() : LanguageEntity.codes(languages);
    }
}
