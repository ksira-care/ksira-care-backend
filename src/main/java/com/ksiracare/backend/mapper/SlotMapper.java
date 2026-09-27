package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.entity.Slot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SlotMapper {

    @Mapping(target = "time", expression = "java(toEpochMilli(slot.getSlotTime()))")
    SlotResponseDto slotToResponseDto(Slot slot);

    List<SlotResponseDto> slotsToResponseDtos(List<Slot> slots);

    default Long toEpochMilli(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    default LocalDateTime toLocalDateTime(Long epoch) {
        if (epoch == null) {
            return null;
        }
        // Auto-detect seconds vs milliseconds (10-digit threshold: 10_000_000_000L)
        if (epoch < 10_000_000_000L) {
            return Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC).toLocalDateTime();
        }
        return Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDateTime();
    }
}
