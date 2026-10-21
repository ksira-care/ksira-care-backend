package com.ksiracare.backend.security;

import com.ksiracare.backend.entity.Account;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.AccountRepository;
import com.ksiracare.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Finds the accounts behind each {@link Role}, so sign-in and session checks are written once. */
@Component
@RequiredArgsConstructor
public class AccountDirectory {

    private final AaptaRepository aaptaRepository;
    private final AdminRepository adminRepository;

    public Optional<? extends Account> findByEmail(Role role, String email) {
        return accounts(role).findByEmailIgnoreCase(email);
    }

    public Optional<? extends Account> findById(Role role, UUID accountId) {
        return accounts(role).findById(accountId);
    }

    /**
     * Whether a session issued under {@code sessionVersion} still stands: the account exists, is
     * active, and hasn't ended its sessions since (e.g. by changing its password).
     */
    public boolean isSessionCurrent(Role role, UUID accountId, int sessionVersion) {
        return accounts(role).existsByIdAndActiveTrueAndSessionVersion(accountId, sessionVersion);
    }

    // A switch rather than a map: adding a Role doesn't compile until it is wired up here.
    private AccountRepository<? extends Account> accounts(Role role) {
        return switch (role) {
            case AAPTA -> aaptaRepository;
            case ADMIN -> adminRepository;
        };
    }
}
