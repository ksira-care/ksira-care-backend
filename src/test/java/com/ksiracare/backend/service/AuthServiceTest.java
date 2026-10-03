package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.exception.AccountDisabledException;
import com.ksiracare.backend.exception.InvalidCredentialsException;
import com.ksiracare.backend.exception.TooManyLoginAttemptsException;
import com.ksiracare.backend.repository.TherapistRepository;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.LoginAttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final TherapistRepository repository = mock(TherapistRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);
    private AuthService authService;
    private Therapist aanya;

    @BeforeEach
    void setUp() {
        authService = new AuthService(repository, encoder, jwtService, limiter);
        aanya = new Therapist("Aanya", "Mehta", "therapist@ksiracare.com", encoder.encode("password123"));
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.findByEmailIgnoreCase("therapist@ksiracare.com")).thenReturn(Optional.of(aanya));
        when(jwtService.issue(any(), anyString())).thenReturn("signed-token");
    }

    private static LoginRequestDto request(String email, String password) {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void correctDetailsStartASession() {
        AuthService.LoginResult result = authService.login(request("therapist@ksiracare.com", "password123"));

        assertThat(result.email()).isEqualTo("therapist@ksiracare.com");
        assertThat(result.sessionToken()).isEqualTo("signed-token");
        verify(limiter).recordSuccess("therapist@ksiracare.com");
    }

    @Test
    void unknownEmailAndWrongPasswordFailIdentically() {
        assertThatThrownBy(() -> authService.login(request("therapist@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");
        assertThatThrownBy(() -> authService.login(request("nobody@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");

        verify(limiter).recordFailure("therapist@ksiracare.com");
        verify(limiter).recordFailure("nobody@ksiracare.com");
    }

    @Test
    void aDisabledAccountIsOnlyRevealedWithTheRightPassword() {
        aanya.setActive(false);

        assertThatThrownBy(() -> authService.login(request("therapist@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(request("therapist@ksiracare.com", "password123")))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void aBlockedEmailIsRefusedWithoutCheckingThePassword() {
        when(limiter.isBlocked("therapist@ksiracare.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request("therapist@ksiracare.com", "password123")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        verify(jwtService, never()).issue(any(UUID.class), anyString());
    }
}
