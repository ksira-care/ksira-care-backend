package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.request.ChangePasswordRequestDto;
import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.dto.response.SessionDto;
import com.ksiracare.backend.security.SessionCookies;
import com.ksiracare.backend.security.SessionPrincipal;
import com.ksiracare.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One sign-in for aaptas and admins alike; the {@code role} in the answer says which it was. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionCookies sessionCookies;

    /** Starts a session: sets the httpOnly session cookie and returns who signed in, and as what. */
    @PostMapping("/login")
    public ResponseEntity<SessionDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.issue(result.sessionToken()).toString())
                .body(SessionDto.of(result.principal()));
    }

    /** Ends the session by clearing the cookie. Always succeeds, even if already signed out. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.clear().toString())
                .build();
    }

    /** Who is signed in, and as what; 401 if nobody. Lets the sign-in page send them on. */
    @GetMapping("/session")
    public SessionDto session(@AuthenticationPrincipal SessionPrincipal me) {
        return SessionDto.of(me);
    }

    /**
     * Changes the signed-in account's password, e.g. after an aapta first signs in with the one an
     * admin gave them. Signs them out everywhere else; this device gets a fresh session cookie.
     * 422 CURRENT_PASSWORD_INCORRECT if the current password is wrong.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal SessionPrincipal me,
                                               @Valid @RequestBody ChangePasswordRequestDto request) {
        String token = authService.changePassword(me, request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.issue(token).toString())
                .build();
    }
}
