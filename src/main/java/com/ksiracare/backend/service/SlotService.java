package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.time.UtcRange;

import java.util.List;
import java.util.UUID;

public interface SlotService {

    /** Every session hour in the range; hours with no record are reported as unavailable. */
    List<SlotResponseDto> getSlots(UUID therapistId, UtcRange range);

    /** Opens or closes only the hours sent; other hours are left as they are. */
    List<SlotResponseDto> saveSlots(UUID therapistId, UtcRange range, List<SlotRequestDto> changes);
}
