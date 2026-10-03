package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.time.EpochTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface SlotMapper {

    @Mapping(target = "time", expression = "java(toMillis(slot.getSlotTime()))")
    SlotResponseDto toResponse(Slot slot);

    default Long toMillis(LocalDateTime utc) {
        return EpochTime.toMillis(utc);
    }
}
