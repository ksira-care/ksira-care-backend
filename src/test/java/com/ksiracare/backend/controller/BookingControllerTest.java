package com.ksiracare.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksiracare.backend.dto.request.UpdateBookingStatusRequestDto;
import com.ksiracare.backend.dto.response.BookingItemDto;
import com.ksiracare.backend.dto.response.BookingsResponseDto;
import com.ksiracare.backend.enums.BookingStatus;
import com.ksiracare.backend.exception.GlobalExceptionHandler;
import com.ksiracare.backend.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /bookings should return 200 OK with therapist stats and bookings")
    void testGetBookingsEndpoint() throws Exception {
        UUID therapistId = UUID.randomUUID();
        Long startEpoch = 1790499600000L;
        Long endEpoch = 1790506800000L;

        BookingsResponseDto response = BookingsResponseDto.builder()
                .therapistName("Anya M.")
                .sessionsThisMonth(14)
                .todaysSessions(2)
                .completedSessions(50)
                .bookings(List.of(
                        BookingItemDto.builder()
                                .bookingId(UUID.randomUUID())
                                .startTime(startEpoch)
                                .endTime(endEpoch)
                                .customerName("Sara L.")
                                .customerCountry("United Kingdom")
                                .bookingReason("Stress")
                                .bookingStatus(BookingStatus.PENDING)
                                .therapistFee(BigDecimal.valueOf(600.00))
                                .build()
                ))
                .build();

        when(bookingService.getBookings(eq(therapistId), eq(startEpoch), eq(endEpoch), any()))
                .thenReturn(response);

        mockMvc.perform(get("/bookings")
                        .param("therapistId", therapistId.toString())
                        .param("startTime", startEpoch.toString())
                        .param("endTime", endEpoch.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.therapistName").value("Anya M."))
                .andExpect(jsonPath("$.sessionsThisMonth").value(14))
                .andExpect(jsonPath("$.todaysSessions").value(2))
                .andExpect(jsonPath("$.completedSessions").value(50))
                .andExpect(jsonPath("$.bookings.length()").value(1))
                .andExpect(jsonPath("$.bookings[0].customerName").value("Sara L."));
    }

    @Test
    @DisplayName("PATCH /bookings/{id}/status should update status and return 200 OK")
    void testUpdateBookingStatusEndpoint() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UpdateBookingStatusRequestDto request = new UpdateBookingStatusRequestDto(BookingStatus.COMPLETED);

        BookingItemDto response = BookingItemDto.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatus.COMPLETED)
                .build();

        when(bookingService.updateBookingStatus(eq(bookingId), eq(BookingStatus.COMPLETED)))
                .thenReturn(response);

        mockMvc.perform(patch("/bookings/{bookingId}/status", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.bookingStatus").value("COMPLETED"));
    }
}
