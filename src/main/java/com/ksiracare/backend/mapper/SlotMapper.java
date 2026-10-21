package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CommonMappings.class)
public interface SlotMapper {

    @Mapping(source = "slotTime", target = "time")
    SlotResponseDto toResponse(Slot slot);
}
