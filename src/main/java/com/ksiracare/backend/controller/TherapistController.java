package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.DashboardSummaryDto;
import com.ksiracare.backend.dto.response.TherapistProfileDto;
import com.ksiracare.backend.security.TherapistPrincipal;
import com.ksiracare.backend.service.DashboardService;
import com.ksiracare.backend.service.TherapistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** "Me" endpoints: the therapist always comes from the session, never from the URL. */
@RestController
@RequestMapping("/therapists/me")
@RequiredArgsConstructor
public class TherapistController {

    private final TherapistService therapistService;
    private final DashboardService dashboardService;

    @GetMapping
    public TherapistProfileDto getProfile(@AuthenticationPrincipal TherapistPrincipal me) {
        return therapistService.getProfile(me.therapistId());
    }

    @GetMapping("/dashboard-summary")
    public DashboardSummaryDto getDashboardSummary(@AuthenticationPrincipal TherapistPrincipal me) {
        return dashboardService.getSummary(me.therapistId());
    }
}
