package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.TherapistResponseDto;
import com.ksiracare.backend.entity.Therapist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Objects;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface TherapistMapper {

    @Mapping(target = "fullName", expression = "java(buildFullName(therapist))")
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
}
