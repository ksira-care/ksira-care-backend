package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.DashboardSummaryDto;
import com.ksiracare.backend.dto.response.AaptaProfileDto;
import com.ksiracare.backend.security.AaptaPrincipal;
import com.ksiracare.backend.service.DashboardService;
import com.ksiracare.backend.service.AaptaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/dashboard-summary")
    public DashboardSummaryDto getDashboardSummary(@AuthenticationPrincipal AaptaPrincipal me) {
        return dashboardService.getSummary(me.aaptaId());
    }
}
