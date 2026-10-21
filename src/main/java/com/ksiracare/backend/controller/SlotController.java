package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotsResponseDto;
import com.ksiracare.backend.security.AaptaPrincipal;
import com.ksiracare.backend.service.SlotService;
import com.ksiracare.backend.time.QueryRanges;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The signed-in aapta's opening hours. Ranges are [startTime, endTime), epoch milliseconds. */
@RestController
@RequestMapping("/aaptas/me/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;
    private final QueryRanges queryRanges;

    @GetMapping
    public SlotsResponseDto getSlots(@AuthenticationPrincipal AaptaPrincipal me,
                                     @RequestParam long startTime,
                                     @RequestParam long endTime) {
        return new SlotsResponseDto(slotService.getSlots(me.aaptaId(), queryRanges.of(startTime, endTime)));
    }

    /** Applies only the hours sent; 409 SLOT_BOOKED if one of them has a session. */
    @PostMapping
    public SlotsResponseDto saveSlots(@AuthenticationPrincipal AaptaPrincipal me,
                                      @RequestParam long startTime,
                                      @RequestParam long endTime,
                                      @Valid @RequestBody List<@Valid SlotRequestDto> changes) {
        return new SlotsResponseDto(slotService.saveSlots(me.aaptaId(), queryRanges.of(startTime, endTime), changes));
    }
}
