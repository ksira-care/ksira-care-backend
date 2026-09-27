package com.ksiracare.backend.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingsResponseDto {

    private String therapistName;
    private long sessionsThisMonth;
    private long todaysSessions;
    private long completedSessions;
    private List<BookingItemDto> bookings;
}
