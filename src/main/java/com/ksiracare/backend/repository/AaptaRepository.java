package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Aapta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AaptaRepository extends JpaRepository<Aapta, UUID> {

    Optional<Aapta> findByEmailIgnoreCase(String email);

    /** For the profile: loads languages in the same query. */
    @Query("SELECT t FROM Aapta t LEFT JOIN FETCH t.languages WHERE t.id = :id")
    Optional<Aapta> findWithLanguagesById(@Param("id") UUID id);
}
