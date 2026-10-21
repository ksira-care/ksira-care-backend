package com.ksiracare.backend.mapper;

import com.ksiracare.backend.dto.response.AaptaRefDto;
import com.ksiracare.backend.dto.response.AdminBookingDto;
import com.ksiracare.backend.dto.response.BookingFormDto;
import com.ksiracare.backend.dto.response.ClientDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", uses = CommonMappings.class)
public interface AdminBookingMapper {

    @Mapping(source = "id", target = "aaptaId")
    AaptaRefDto toRef(Aapta aapta);

    @Mapping(source = "id", target = "bookingId")
    @Mapping(source = "customerName", target = "name")
    @Mapping(source = "customerEmail", target = "email")
    @Mapping(source = "customerPreferredLanguages", target = "languages")
    @Mapping(source = "bookingReason", target = "notes")
    @Mapping(target = "consent", expression = "java(booking.getConsentGivenAt() != null)")
    @Mapping(source = "createdAt", target = "submittedAt")
    BookingFormDto toForm(Booking booking);

    /** A day-view row; what the admin may do, and the booking's history, are worked out by the caller. */
    default AdminBookingDto toRow(Booking booking, Aapta previousAapta, String cancelReason,
                                  boolean editable, LocalDateTime editableFrom, boolean cancellable) {
        return new AdminBookingDto(
                booking.getId(),
                CommonMappings.toMillis(booking.getStartTime()),
                CommonMappings.toMillis(booking.getEndTime()),
                new ClientDto(booking.getCustomerName(), booking.getCustomerEmail()),
                CommonMappings.toLanguages(booking.getCustomerPreferredLanguages()),
                booking.getAapta() == null ? null : toRef(booking.getAapta()),
                previousAapta == null ? null : toRef(previousAapta),
                booking.isBccEmailSent(),
                booking.getStatus(),
                CommonMappings.toMillis(booking.getMarkedAt()),
                editable,
                CommonMappings.toMillis(editableFrom),
                cancellable,
                cancelReason,
                booking.getVersion());
    }
}
