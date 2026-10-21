package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.ChangePasswordRequestDto;
import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.dto.response.SessionDto;
import com.ksiracare.backend.entity.Account;
import com.ksiracare.backend.exception.AccountDisabledException;
import com.ksiracare.backend.exception.CurrentPasswordIncorrectException;
import com.ksiracare.backend.exception.InvalidCredentialsException;
import com.ksiracare.backend.exception.ResourceNotFoundException;
import com.ksiracare.backend.exception.TooManyLoginAttemptsException;
import com.ksiracare.backend.security.AccountDirectory;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.LoginAttemptLimiter;
import com.ksiracare.backend.security.SessionPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Checks an email and password and starts a session. There is one sign-in for everyone: the
 * account the email belongs to decides whether the session is an aapta's or an admin's.
 * <ul>
 *   <li>Unknown email and wrong password give the same error, and take the same time,
 *       so the endpoint can't be used to find out who has an account.</li>
 *   <li>The account-disabled message is only shown once the password is right.</li>
 *   <li>Repeated failures are rate-limited per email.</li>
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

    /**
     * @param session      who signed in, and as what
     * @param sessionToken the token for the session cookie
     */
    public record LoginResult(SessionDto session, String sessionToken) {
    }

    /** Who {@code me} is, for the app to greet them and open their portal. */
    @Transactional(readOnly = true)
    public SessionDto describe(SessionPrincipal me) {
        return SessionDto.of(me.role(), findAccount(me));
    }

    /**
     * Changes the signed-in account's own password; the current one must be given. Every session
     * of the account ends, on every device.
     *
     * @return a new session token for the device that made the change, so it stays signed in
     */
    @Transactional
    public String changePassword(SessionPrincipal me, ChangePasswordRequestDto request) {
        Account account = findAccount(me);
        if (!passwordEncoder.matches(request.currentPassword(), account.getPassword())) {
            throw new CurrentPasswordIncorrectException("Your current password is incorrect.");
        }
        account.changePassword(passwordEncoder.encode(request.newPassword()));
        return jwtService.issue(me, account.getSessionVersion());
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequestDto request) {
        String email = request.email().trim();
        if (attemptLimiter.isBlocked(email)) {
            throw new TooManyLoginAttemptsException("Too many sign-in attempts. Please wait a few minutes and try again.");
        }

        AccountDirectory.RoleAccount match = passwordMatch(accounts.findByEmail(email), request.password());
        if (match == null) {
            attemptLimiter.recordFailure(email);
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }
        Account account = match.account();
        if (!account.isActive()) {
            throw new AccountDisabledException("This account is inactive. Please contact your coordinator.");
        }

        attemptLimiter.recordSuccess(email);
        SessionPrincipal principal = SessionPrincipal.of(match.role(), account.getId(), account.getEmail());
        return new LoginResult(SessionDto.of(match.role(), account), jwtService.issue(principal, account.getSessionVersion()));
    }

    private Account findAccount(SessionPrincipal me) {
        return accounts.findById(me.role(), me.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }

    /** The account whose password this is, or null. An unknown email still costs one hash check. */
    private AccountDirectory.RoleAccount passwordMatch(List<AccountDirectory.RoleAccount> candidates, String password) {
        if (candidates.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            return null;
        }
        return candidates.stream()
                .filter(candidate -> passwordEncoder.matches(password, candidate.account().getPassword()))
                .findFirst()
                .orElse(null);
    }
}
