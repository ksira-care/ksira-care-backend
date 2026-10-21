package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.dto.response.AdminLoginResponseDto;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.Role;
import com.ksiracare.backend.security.SessionCookies;
import com.ksiracare.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin sign-in. Same rules as aapta sign-in, but a separate account list and session cookie. */
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final SessionCookies sessionCookies;
    private final JwtService jwtService;

    /** Starts an admin session: sets the httpOnly admin session cookie and returns who signed in. */
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthService.LoginResult result = authService.login(Role.ADMIN, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.issue(Role.ADMIN, result.sessionToken(), jwtService.ttl()).toString())
                .body(new AdminLoginResponseDto(result.principal().accountId(), result.principal().email()));
    }

    /** Ends the admin session by clearing its cookie. Always succeeds, even if already signed out. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.clear(Role.ADMIN).toString())
                .build();
    }
}
