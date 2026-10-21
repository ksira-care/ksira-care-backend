package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Admin;
import com.ksiracare.backend.exception.AccountDisabledException;
import com.ksiracare.backend.exception.InvalidCredentialsException;
import com.ksiracare.backend.exception.TooManyLoginAttemptsException;
import com.ksiracare.backend.security.AaptaPrincipal;
import com.ksiracare.backend.security.AccountDirectory;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.LoginAttemptLimiter;
import com.ksiracare.backend.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AccountDirectory accounts = mock(AccountDirectory.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final LoginAttemptLimiter limiter = mock(LoginAttemptLimiter.class);
    private AuthService authService;
    private Aapta aanya;
    private Admin meera;

    @BeforeEach
    void setUp() {
        authService = new AuthService(accounts, encoder, jwtService, limiter);
        aanya = new Aapta("Aanya", "Mehta", "aapta@ksiracare.com", encoder.encode("password123"));
        meera = new Admin("Meera", "Iyer", "admin@ksiracare.com", encoder.encode("admin-password"));
        doReturn(Optional.empty()).when(accounts).findByEmail(any(), anyString());
        doReturn(Optional.of(aanya)).when(accounts).findByEmail(Role.AAPTA, "aapta@ksiracare.com");
        doReturn(Optional.of(meera)).when(accounts).findByEmail(Role.ADMIN, "admin@ksiracare.com");
        when(jwtService.issue(any())).thenReturn("signed-token");
    }

    private static LoginRequestDto request(String email, String password) {
        return new LoginRequestDto(email, password);
    }

    @Test
    void correctDetailsStartASession() {
        AuthService.LoginResult result = authService.login(Role.AAPTA, request("aapta@ksiracare.com", "password123"));

        assertThat(result.principal()).isInstanceOf(AaptaPrincipal.class);
        assertThat(result.principal().email()).isEqualTo("aapta@ksiracare.com");
        assertThat(result.sessionToken()).isEqualTo("signed-token");
        verify(limiter).recordSuccess("AAPTA:aapta@ksiracare.com");
    }

    @Test
    void adminsSignInAsAdmins() {
        AuthService.LoginResult result = authService.login(Role.ADMIN, request("admin@ksiracare.com", "admin-password"));

        assertThat(result.principal()).isInstanceOf(AdminPrincipal.class);
        verify(jwtService).issue(result.principal());
    }

    @Test
    void anAccountCanOnlySignInThroughItsOwnRole() {
        assertThatThrownBy(() -> authService.login(Role.ADMIN, request("aapta@ksiracare.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(Role.AAPTA, request("admin@ksiracare.com", "admin-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unknownEmailAndWrongPasswordFailIdentically() {
        assertThatThrownBy(() -> authService.login(Role.AAPTA, request("aapta@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");
        assertThatThrownBy(() -> authService.login(Role.AAPTA, request("nobody@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");

        verify(limiter).recordFailure("AAPTA:aapta@ksiracare.com");
        verify(limiter).recordFailure("AAPTA:nobody@ksiracare.com");
    }

    @Test
    void aDisabledAccountIsOnlyRevealedWithTheRightPassword() {
        meera.setActive(false);

        assertThatThrownBy(() -> authService.login(Role.ADMIN, request("admin@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(Role.ADMIN, request("admin@ksiracare.com", "admin-password")))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void aBlockedEmailIsRefusedWithoutCheckingThePassword() {
        when(limiter.isBlocked("ADMIN:admin@ksiracare.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(Role.ADMIN, request("admin@ksiracare.com", "admin-password")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        verify(jwtService, never()).issue(any());
    }
}
