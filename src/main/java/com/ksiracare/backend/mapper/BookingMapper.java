package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.enums.Language;
import com.ksiracare.backend.time.EpochTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "id", target = "bookingId")
    @Mapping(source = "status", target = "bookingStatus")
    @Mapping(source = "rescheduleReason", target = "rescheduleNote")
    @Mapping(target = "rescheduledFrom", expression = "java(booking.isRescheduled() ? toMillis(booking.getPreviousStartTime()) : null)")
    @Mapping(target = "customerPreferredLanguages", expression = "java(toLanguages(booking.getCustomerPreferredLanguages()))")
    BookingItemDto toItem(Booking booking);

    List<BookingItemDto> toItems(List<Booking> bookings);

    default Long toMillis(LocalDateTime utc) {
        return EpochTime.toMillis(utc);
    }

    default Set<Language> toLanguages(Set<LanguageEntity> languages) {
        return languages == null ? Set.of() : languages.stream().map(LanguageEntity::getCode).collect(Collectors.toSet());
    }
}
