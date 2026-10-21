package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.CreateAaptaRequestDto;
import com.ksiracare.backend.dto.response.AaptaRemovedDto;
import com.ksiracare.backend.dto.response.AaptaSummaryDto;
import com.ksiracare.backend.dto.response.AaptasResponseDto;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.service.AdminAaptaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** The admin roster of aaptas. */
@RestController
@RequestMapping("/admin/aaptas")
@RequiredArgsConstructor
public class AdminAaptaController {

    private final AdminAaptaService adminAaptaService;

    @GetMapping
    public AaptasResponseDto getRoster() {
        return new AaptasResponseDto(adminAaptaService.getRoster());
    }

    /** "Add an Aapta": 409 EMAIL_TAKEN if the email already has an account. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AaptaSummaryDto create(@Valid @RequestBody CreateAaptaRequestDto request) {
        return adminAaptaService.create(request);
    }

    /** Removes the aapta: their login stops at once and upcoming sessions go back to unallocated. */
    @DeleteMapping("/{aaptaId}")
    public AaptaRemovedDto remove(@AuthenticationPrincipal AdminPrincipal me, @PathVariable UUID aaptaId) {
        return adminAaptaService.remove(me, aaptaId);
    }
}
