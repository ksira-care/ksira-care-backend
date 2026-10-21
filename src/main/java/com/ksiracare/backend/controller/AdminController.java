package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.AdminProfileDto;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in admin, taken from the admin session. */
@RestController
@RequestMapping("/admin/me")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** Also the admin console's "am I signed in?" check (401 if not). */
    @GetMapping
    public AdminProfileDto getProfile(@AuthenticationPrincipal AdminPrincipal me) {
        return adminService.getProfile(me.adminId());
    }
}
