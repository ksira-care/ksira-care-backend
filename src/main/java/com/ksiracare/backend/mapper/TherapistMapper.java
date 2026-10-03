package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.TherapistProfileDto;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.enums.Language;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TherapistMapper {

    @Mapping(source = "id", target = "therapistId")
    @Mapping(target = "languages", expression = "java(toLanguages(therapist.getLanguages()))")
    TherapistProfileDto toProfile(Therapist therapist);

    default Set<Language> toLanguages(Set<LanguageEntity> languages) {
        return languages == null ? Set.of() : languages.stream().map(LanguageEntity::getCode).collect(Collectors.toSet());
    }
}
