package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.AaptaProfileDto;
import com.ksiracare.backend.dto.response.AaptaSummaryDto;
import com.ksiracare.backend.entity.Aapta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CommonMappings.class)
public interface AaptaMapper {

    @Mapping(source = "id", target = "aaptaId")
    AaptaProfileDto toProfile(Aapta aapta);

    @Mapping(source = "id", target = "aaptaId")
    AaptaSummaryDto toSummary(Aapta aapta);
}
