package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.enums.Language;
import lombok.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingItemDto {

    private UUID bookingId;
    private Long startTime; // Epoch millis UTC
    private Long endTime;   // Epoch millis UTC
    private String customerName;
    private String customerCountry;
    private String bookingReason;
    private BookingStatus bookingStatus;
    private Set<Language> customerPreferredLanguages;
    private BigDecimal therapistFee;

    // Structured Audit Fields
    private Long assignedAt;          // Epoch millis UTC
    @com.fasterxml.jackson.annotation.JsonProperty("isRescheduled")
    private boolean rescheduled;
    private Long previousStartTime;   // Epoch millis UTC
    private String rescheduleReason;
}
