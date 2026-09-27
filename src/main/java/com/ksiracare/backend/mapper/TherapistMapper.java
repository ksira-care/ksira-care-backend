package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.TherapistResponseDto;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.enums.Language;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface TherapistMapper {

    @Mapping(target = "fullName", expression = "java(buildFullName(therapist))")
    @Mapping(target = "languages", expression = "java(mapLanguageEntities(therapist.getLanguages()))")
    TherapistResponseDto therapistToDto(Therapist therapist);

    default String buildFullName(Therapist therapist) {
        return Stream.of(
                        therapist.getFirstName(),
                        therapist.getMiddleName(),
                        therapist.getLastName()
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .reduce((first, second) -> first + " " + second)
                .orElse("");
    }

    default Set<Language> mapLanguageEntities(Set<LanguageEntity> languageEntities) {
        if (languageEntities == null) {
            return null;
        }
        return languageEntities.stream()
                .map(LanguageEntity::getCode)
                .collect(Collectors.toSet());
    }
}
