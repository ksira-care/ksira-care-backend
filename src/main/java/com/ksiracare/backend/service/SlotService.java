package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;

import java.util.List;
import java.util.UUID;

public interface SlotService {

    List<SlotResponseDto> getSlots(UUID therapistId, Long startTime, Long endTime);

    List<SlotResponseDto> saveSlots(UUID therapistId, Long startTime, Long endTime, List<SlotRequestDto> slots);
}
