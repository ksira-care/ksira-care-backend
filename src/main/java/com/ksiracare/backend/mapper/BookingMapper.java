package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = CommonMappings.class)
public interface BookingMapper {

    @Mapping(source = "id", target = "bookingId")
    @Mapping(source = "status", target = "bookingStatus")
    @Mapping(source = "rescheduleReason", target = "rescheduleNote")
    @Mapping(target = "rescheduledFrom",
            expression = "java(booking.isRescheduled() ? CommonMappings.toMillis(booking.getPreviousStartTime()) : null)")
    BookingItemDto toItem(Booking booking);

    List<BookingItemDto> toItems(List<Booking> bookings);
}
