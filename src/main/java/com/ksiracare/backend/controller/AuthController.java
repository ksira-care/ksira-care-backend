package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.dto.response.LoginResponseDto;
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

/** Aapta sign-in. Admins sign in through {@link AdminAuthController}. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionCookies sessionCookies;
    private final JwtService jwtService;

    /** Starts a session: sets the httpOnly session cookie and returns who signed in. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthService.LoginResult result = authService.login(Role.AAPTA, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.issue(Role.AAPTA, result.sessionToken(), jwtService.ttl()).toString())
                .body(new LoginResponseDto(result.principal().accountId(), result.principal().email()));
    }

    /** Ends the session by clearing the cookie. Always succeeds, even if already signed out. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.clear(Role.AAPTA).toString())
                .build();
    }
}
