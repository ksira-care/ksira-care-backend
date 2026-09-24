package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.TherapistResponseDto;
import com.ksiracare.backend.service.TherapistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/therapists")
@RequiredArgsConstructor
public class TherapistController {
    private final TherapistService therapistService;

    @GetMapping("/{id}")
    public ResponseEntity<TherapistResponseDto> getTherapistById(@PathVariable UUID id) {
        return ResponseEntity.ok(therapistService.getTherapistById(id));
    }
}
