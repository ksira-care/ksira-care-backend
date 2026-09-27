package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.SlotRequestDto;
import com.ksiracare.backend.dto.response.SlotResponseDto;
import com.ksiracare.backend.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping
    public ResponseEntity<List<SlotResponseDto>> getSlots(
            @RequestParam UUID therapistId,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        return ResponseEntity.ok(slotService.getSlots(therapistId, startTime, endTime));
    }

    @PostMapping
    public ResponseEntity<List<SlotResponseDto>> saveSlots(
            @RequestParam UUID therapistId,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @Valid @RequestBody List<SlotRequestDto> slots) {
        return ResponseEntity.ok(slotService.saveSlots(therapistId, startTime, endTime, slots));
    }
}
