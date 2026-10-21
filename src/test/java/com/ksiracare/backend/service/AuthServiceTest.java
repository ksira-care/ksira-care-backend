package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.entity.Admin;
import com.ksiracare.backend.exception.AccountDisabledException;
import com.ksiracare.backend.exception.InvalidCredentialsException;
import com.ksiracare.backend.exception.TooManyLoginAttemptsException;
import com.ksiracare.backend.security.AccountDirectory;
import com.ksiracare.backend.security.AccountDirectory.RoleAccount;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.LoginAttemptLimiter;
import com.ksiracare.backend.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        when(accounts.findByEmail(anyString())).thenReturn(List.of());
        when(accounts.findByEmail("aapta@ksiracare.com")).thenReturn(List.of(new RoleAccount(Role.AAPTA, aanya)));
        when(accounts.findByEmail("admin@ksiracare.com")).thenReturn(List.of(new RoleAccount(Role.ADMIN, meera)));
        when(jwtService.issue(any(), anyInt())).thenReturn("signed-token");
    }

    private static LoginRequestDto request(String email, String password) {
        return new LoginRequestDto(email, password);
    }

    @Test
    void correctDetailsStartASession() {
        AuthService.LoginResult result = authService.login(request("aapta@ksiracare.com", "password123"));

        assertThat(result.session().role()).isEqualTo(Role.AAPTA);
        assertThat(result.session().email()).isEqualTo("aapta@ksiracare.com");
        assertThat(result.session().firstName()).isEqualTo("Aanya");
        assertThat(result.sessionToken()).isEqualTo("signed-token");
        verify(limiter).recordSuccess("aapta@ksiracare.com");
    }

    @Test
    void adminsSignInAsAdmins() {
        AuthService.LoginResult result = authService.login(request("admin@ksiracare.com", "admin-password"));

        assertThat(result.session().role()).isEqualTo(Role.ADMIN);
        verify(jwtService).issue(any(AdminPrincipal.class), eq(0));
    }

    @Test
    void ifAnEmailWasGivenToBothRolesThePasswordDecides() {
        Aapta sameEmail = new Aapta("Meera", "Iyer", "admin@ksiracare.com", encoder.encode("aapta-password"));
        when(accounts.findByEmail("admin@ksiracare.com")).thenReturn(List.of(
                new RoleAccount(Role.AAPTA, sameEmail), new RoleAccount(Role.ADMIN, meera)));

        assertThat(authService.login(request("admin@ksiracare.com", "admin-password")).session().role())
                .isEqualTo(Role.ADMIN);
        assertThat(authService.login(request("admin@ksiracare.com", "aapta-password")).session().role())
                .isEqualTo(Role.AAPTA);
    }

    @Test
    void unknownEmailAndWrongPasswordFailIdentically() {
        assertThatThrownBy(() -> authService.login(request("aapta@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");
        assertThatThrownBy(() -> authService.login(request("nobody@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect.");

        verify(limiter).recordFailure("aapta@ksiracare.com");
        verify(limiter).recordFailure("nobody@ksiracare.com");
    }

    @Test
    void aDisabledAccountIsOnlyRevealedWithTheRightPassword() {
        meera.setActive(false);

        assertThatThrownBy(() -> authService.login(request("admin@ksiracare.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> authService.login(request("admin@ksiracare.com", "admin-password")))
                .isInstanceOf(AccountDisabledException.class);
    }

    @Test
    void aBlockedEmailIsRefusedWithoutCheckingThePassword() {
        when(limiter.isBlocked("admin@ksiracare.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request("admin@ksiracare.com", "admin-password")))
                .isInstanceOf(TooManyLoginAttemptsException.class);
        verify(jwtService, never()).issue(any(), anyInt());
    }
}
