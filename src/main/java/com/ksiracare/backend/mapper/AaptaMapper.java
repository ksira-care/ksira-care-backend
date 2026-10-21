package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.AaptaProfileDto;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.enums.Language;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AaptaMapper {

    @Mapping(source = "id", target = "aaptaId")
    @Mapping(target = "languages", expression = "java(toLanguages(aapta.getLanguages()))")
    AaptaProfileDto toProfile(Aapta aapta);

    default Set<Language> toLanguages(Set<LanguageEntity> languages) {
        return languages == null ? Set.of() : languages.stream().map(LanguageEntity::getCode).collect(Collectors.toSet());
    }
}
