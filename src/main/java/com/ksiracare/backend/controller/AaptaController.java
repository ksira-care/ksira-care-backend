package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.UpdateAaptaRequestDto;
import com.ksiracare.backend.dto.response.DashboardSummaryDto;
import com.ksiracare.backend.dto.response.AaptaProfileDto;
import com.ksiracare.backend.security.AaptaPrincipal;
import com.ksiracare.backend.service.DashboardService;
import com.ksiracare.backend.service.AaptaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Me" endpoints: the aapta always comes from the session, never from the URL. */
@RestController
@RequestMapping("/aaptas/me")
@RequiredArgsConstructor
public class AaptaController {

    private final AaptaService aaptaService;
    private final DashboardService dashboardService;

    @GetMapping
    public AaptaProfileDto getProfile(@AuthenticationPrincipal AaptaPrincipal me) {
        return aaptaService.getProfile(me.aaptaId());
    }

    /**
     * Edits their own profile — the same format admins use. Aaptas may change their name,
     * phone, date of birth, address and languages; their email is changed by an admin
     * (403 FIELD_NOT_EDITABLE).
     */
    @PatchMapping
    public AaptaProfileDto updateProfile(@AuthenticationPrincipal AaptaPrincipal me,
                                         @Valid @RequestBody UpdateAaptaRequestDto request) {
        return aaptaService.update(me, me.aaptaId(), request);
    }

    @GetMapping("/dashboard-summary")
    public DashboardSummaryDto getDashboardSummary(@AuthenticationPrincipal AaptaPrincipal me) {
        return dashboardService.getSummary(me.aaptaId());
    }
}
