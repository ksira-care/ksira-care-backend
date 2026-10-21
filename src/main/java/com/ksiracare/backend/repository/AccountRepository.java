package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

/** Queries every kind of {@link Account} supports, so sign-in works the same for all of them. */
@NoRepositoryBean
public interface AccountRepository<T extends Account> extends JpaRepository<T, UUID> {

    Optional<T> findByEmailIgnoreCase(String email);

    /** Whether any account, active or not, already signs in with this email. */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Cheap per-request check that a session is still good: the account exists, hasn't been
     * deactivated, and the session was issued under its current session version.
     */
    boolean existsByIdAndActiveTrueAndSessionVersion(UUID id, int sessionVersion);
}
