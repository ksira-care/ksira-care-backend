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

    /** Whether the account still exists and hasn't been deactivated since the session began. */
    public boolean isActive(Role role, UUID accountId) {
        return accounts(role).existsByIdAndActiveTrue(accountId);
    }

    // A switch rather than a map: adding a Role doesn't compile until it is wired up here.
    private AccountRepository<? extends Account> accounts(Role role) {
        return switch (role) {
            case AAPTA -> aaptaRepository;
            case ADMIN -> adminRepository;
        };
    }
}
