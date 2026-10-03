package com.ksiracare.backend.service;

import com.ksiracare.backend.dto.request.LoginRequestDto;
import com.ksiracare.backend.entity.Therapist;
import com.ksiracare.backend.exception.AccountDisabledException;
import com.ksiracare.backend.exception.InvalidCredentialsException;
import com.ksiracare.backend.exception.TooManyLoginAttemptsException;
import com.ksiracare.backend.repository.TherapistRepository;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.LoginAttemptLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Checks a therapist's email and password and starts a session.
 * <ul>
 *   <li>Unknown email and wrong password give the same error, and take the same time,
 *       so the endpoint can't be used to find out who has an account.</li>
 *   <li>The account-disabled message is only shown once the password is right.</li>
 *   <li>Repeated failures are rate-limited per email.</li>
 * </ul>
 */
@Service
public class AuthService {

    private final TherapistRepository therapistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter attemptLimiter;
    /** Compared against when the email is unknown, so both paths cost one hash check. */
    private final String dummyHash;

    public AuthService(TherapistRepository therapistRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptLimiter attemptLimiter) {
        this.therapistRepository = therapistRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.attemptLimiter = attemptLimiter;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public record LoginResult(UUID therapistId, String email, String sessionToken) {
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequestDto request) {
        String email = request.getEmail().trim();
        if (attemptLimiter.isBlocked(email)) {
            throw new TooManyLoginAttemptsException("Too many sign-in attempts. Please wait a few minutes and try again.");
        }

        Optional<Therapist> therapist = therapistRepository.findByEmailIgnoreCase(email);
        String storedHash = therapist.map(Therapist::getPassword).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), storedHash);

        if (therapist.isEmpty() || !passwordMatches) {
            attemptLimiter.recordFailure(email);
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }
        if (!therapist.get().isActive()) {
            throw new AccountDisabledException("This account is inactive. Please contact your coordinator.");
        }

        attemptLimiter.recordSuccess(email);
        Therapist signedIn = therapist.get();
        return new LoginResult(signedIn.getId(), signedIn.getEmail(),
                jwtService.issue(signedIn.getId(), signedIn.getEmail()));
    }
}
