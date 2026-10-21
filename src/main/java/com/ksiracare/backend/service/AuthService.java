package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.entity.Account;
import com.ksiracare.backend.exception.AccountDisabledException;
import com.ksiracare.backend.exception.InvalidCredentialsException;
import com.ksiracare.backend.exception.TooManyLoginAttemptsException;
import com.ksiracare.backend.security.AccountDirectory;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.LoginAttemptLimiter;
import com.ksiracare.backend.security.Role;
import com.ksiracare.backend.security.SessionPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Checks an email and password for one {@link Role} and starts a session.
 * <ul>
 *   <li>Unknown email and wrong password give the same error, and take the same time,
 *       so the endpoint can't be used to find out who has an account.</li>
 *   <li>The account-disabled message is only shown once the password is right.</li>
 *   <li>Repeated failures are rate-limited per role and email.</li>
 * </ul>
 */
@Service
public class AuthService {

    private final AccountDirectory accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter attemptLimiter;
    /** Compared against when the email is unknown, so both paths cost one hash check. */
    private final String dummyHash;

    public AuthService(AccountDirectory accounts,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptLimiter attemptLimiter) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.attemptLimiter = attemptLimiter;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record LoginResult(SessionPrincipal principal, String sessionToken) {
    }

    @Transactional(readOnly = true)
    public LoginResult login(Role role, LoginRequestDto request) {
        String email = request.email().trim();
        String limiterKey = role.name() + ":" + email;
        if (attemptLimiter.isBlocked(limiterKey)) {
            throw new TooManyLoginAttemptsException("Too many sign-in attempts. Please wait a few minutes and try again.");
        }

        Optional<? extends Account> account = accounts.findByEmail(role, email);
        String storedHash = account.map(Account::getPassword).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), storedHash);

        if (account.isEmpty() || !passwordMatches) {
            attemptLimiter.recordFailure(limiterKey);
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }
        if (!account.get().isActive()) {
            throw new AccountDisabledException("This account is inactive. Please contact your coordinator.");
        }

        attemptLimiter.recordSuccess(limiterKey);
        SessionPrincipal principal = SessionPrincipal.of(role, account.get().getId(), account.get().getEmail());
        return new LoginResult(principal, jwtService.issue(principal));
    }
}
