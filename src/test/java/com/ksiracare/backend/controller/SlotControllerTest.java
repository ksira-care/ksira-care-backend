package com.ksiracare.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.enums.SlotStatus;
import com.ksiracare.backend.exception.GlobalExceptionHandler;
import com.ksiracare.backend.exception.InvalidSlotTimeException;
import com.ksiracare.backend.exception.SlotConflictException;
import com.ksiracare.backend.service.SlotService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SlotControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SlotService slotService;

    @InjectMocks
    private SlotController slotController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(slotController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /slots should return 200 OK and slots array")
    void testGetSlotsEndpoint() throws Exception {
        UUID therapistId = UUID.randomUUID();
        Long startEpoch = 1790499600000L; // 2026-09-27T09:00:00Z
        Long endEpoch = 1790506800000L;   // 2026-09-27T11:00:00Z

        List<SlotResponseDto> responseList = List.of(
                SlotResponseDto.builder()
                        .id(null)
                        .time(startEpoch)
                        .status(SlotStatus.THERAPIST_UNAVAILABLE)
                        .build(),
                SlotResponseDto.builder()
                        .id(UUID.randomUUID())
                        .time(1790503200000L)
                        .status(SlotStatus.THERAPIST_AVAILABLE)
                        .build()
        );

        when(slotService.getSlots(eq(therapistId), eq(startEpoch), eq(endEpoch)))
                .thenReturn(responseList);

        mockMvc.perform(get("/slots")
                        .param("therapistId", therapistId.toString())
                        .param("startTime", startEpoch.toString())
                        .param("endTime", endEpoch.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].time").value(startEpoch))
                .andExpect(jsonPath("$[0].status").value("THERAPIST_UNAVAILABLE"))
                .andExpect(jsonPath("$[1].status").value("THERAPIST_AVAILABLE"));
    }

    @Test
    @DisplayName("POST /slots should save slots and return 200 OK")
    void testSaveSlotsEndpoint() throws Exception {
        UUID therapistId = UUID.randomUUID();
        Long startEpoch = 1790499600000L;
        Long endEpoch = 1790506800000L;
        Long slotEpoch = 1790503200000L;

        List<SlotRequestDto> request = List.of(
                new SlotRequestDto(null, slotEpoch, SlotStatus.THERAPIST_AVAILABLE)
        );

        List<SlotResponseDto> response = List.of(
                SlotResponseDto.builder()
                        .id(UUID.randomUUID())
                        .time(slotEpoch)
                        .status(SlotStatus.THERAPIST_AVAILABLE)
                        .build()
        );

        when(slotService.saveSlots(eq(therapistId), eq(startEpoch), eq(endEpoch), any()))
                .thenReturn(response);

        mockMvc.perform(post("/slots")
                        .param("therapistId", therapistId.toString())
                        .param("startTime", startEpoch.toString())
                        .param("endTime", endEpoch.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].time").value(slotEpoch))
                .andExpect(jsonPath("$[0].status").value("THERAPIST_AVAILABLE"));
    }

    @Test
    @DisplayName("POST /slots with conflicting booked slot should return 409 Conflict")
    void testSaveSlotsConflict() throws Exception {
        UUID therapistId = UUID.randomUUID();
        Long startEpoch = 1790499600000L;
        Long endEpoch = 1790506800000L;
        Long slotEpoch = 1790503200000L;

        List<SlotRequestDto> request = List.of(
                new SlotRequestDto(null, slotEpoch, SlotStatus.THERAPIST_AVAILABLE)
        );

        when(slotService.saveSlots(eq(therapistId), eq(startEpoch), eq(endEpoch), any()))
                .thenThrow(new SlotConflictException("Cannot modify already booked slot at: " + slotEpoch));

        mockMvc.perform(post("/slots")
                        .param("therapistId", therapistId.toString())
                        .param("startTime", startEpoch.toString())
                        .param("endTime", endEpoch.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /slots with invalid time should return 400 Bad Request")
    void testSaveSlotsInvalidTime() throws Exception {
        UUID therapistId = UUID.randomUUID();
        Long startEpoch = 1790499600000L;
        Long endEpoch = 1790506800000L;
        Long invalidSlotEpoch = 1790505000000L; // minute != 0

        List<SlotRequestDto> request = List.of(
                new SlotRequestDto(null, invalidSlotEpoch, SlotStatus.THERAPIST_AVAILABLE)
        );

        when(slotService.saveSlots(eq(therapistId), eq(startEpoch), eq(endEpoch), any()))
                .thenThrow(new InvalidSlotTimeException("Invalid slot time"));

        mockMvc.perform(post("/slots")
                        .param("therapistId", therapistId.toString())
                        .param("startTime", startEpoch.toString())
                        .param("endTime", endEpoch.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
