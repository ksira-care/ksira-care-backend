package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Aapta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AaptaRepository extends JpaRepository<Aapta, UUID> {

    Optional<Aapta> findByEmailIgnoreCase(String email);

    /** Cheap per-request check that a signed-in account still exists and hasn't been deactivated. */
    boolean existsByIdAndActiveTrue(UUID id);

    /** For the profile: loads languages in the same query. */
    @Query("SELECT a FROM Aapta a LEFT JOIN FETCH a.languages WHERE a.id = :id")
    Optional<Aapta> findWithLanguagesById(@Param("id") UUID id);
}
