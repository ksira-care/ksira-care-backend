package com.ksiracare.backend.security;

import com.ksiracare.backend.entity.Account;
import com.ksiracare.backend.repository.AaptaRepository;
import com.ksiracare.backend.repository.AccountRepository;
import com.ksiracare.backend.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Finds accounts of every {@link Role}, so sign-in, session checks and email uniqueness are
 * written once. An email signs in to one account only, whatever its role.
 */
@Component
@RequiredArgsConstructor
public class AccountDirectory {

    private final AaptaRepository aaptaRepository;
    private final AdminRepository adminRepository;

    /** An account together with the role it signs in as. */
    public record RoleAccount(Role role, Account account) {
    }

    /**
     * The accounts that sign in with {@code email}: normally none or one. More than one only if
     * the email was given to an aapta and an admin before emails were kept unique across roles.
     */
    public List<RoleAccount> findByEmail(String email) {
        return Arrays.stream(Role.values())
                .flatMap(role -> accounts(role).findByEmailIgnoreCase(email).stream()
                        .map(account -> new RoleAccount(role, account)))
                .toList();
    }

    public Optional<? extends Account> findById(Role role, UUID accountId) {
        return accounts(role).findById(accountId);
    }

    /** Whether an account of any role, other than {@code exceptAccountId}, already signs in with this email. */
    public boolean isEmailTaken(String email, UUID exceptAccountId) {
        return Arrays.stream(Role.values()).anyMatch(role -> exceptAccountId == null
                ? accounts(role).existsByEmailIgnoreCase(email)
                : accounts(role).existsByEmailIgnoreCaseAndIdNot(email, exceptAccountId));
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
