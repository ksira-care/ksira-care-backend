package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.enums.Language;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "id", target = "bookingId")
    @Mapping(target = "startTime", expression = "java(toEpochMilli(booking.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(toEpochMilli(booking.getEndTime()))")
    @Mapping(target = "assignedAt", expression = "java(toEpochMilli(booking.getAssignedAt()))")
    @Mapping(target = "previousStartTime", expression = "java(toEpochMilli(booking.getPreviousStartTime()))")
    @Mapping(source = "status", target = "bookingStatus")
    @Mapping(target = "customerPreferredLanguages", expression = "java(mapLanguageEntities(booking.getCustomerPreferredLanguages()))")
    BookingItemDto bookingToItemDto(Booking booking);

    List<BookingItemDto> bookingsToItemDtos(List<Booking> bookings);

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
        if (epoch < 10_000_000_000L) {
            return Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC).toLocalDateTime();
        }
        return Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC).toLocalDateTime();
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
