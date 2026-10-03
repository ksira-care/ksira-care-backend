package com.ksiracare.backend.dto.request;

import com.ksiracare.backend.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

/** e.g. {@code {"bookingStatus": "COMPLETED"}}; "PENDING" undoes a mark. */
public record UpdateBookingStatusRequestDto(@NotNull(message = "bookingStatus is required") BookingStatus bookingStatus) {
}
