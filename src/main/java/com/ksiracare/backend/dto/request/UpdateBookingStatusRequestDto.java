package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingStatusRequestDto {

    @NotNull(message = "Booking status is required")
    private BookingStatus status;
}
