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

    /** Cheap per-request check that a signed-in account still exists and hasn't been deactivated. */
    boolean existsByIdAndActiveTrue(UUID id);
}
