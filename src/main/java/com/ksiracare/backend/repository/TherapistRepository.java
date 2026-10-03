package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TherapistRepository extends JpaRepository<Therapist, UUID> {

    Optional<Therapist> findByEmailIgnoreCase(String email);

    /** For the profile: loads languages in the same query. */
    @Query("SELECT t FROM Therapist t LEFT JOIN FETCH t.languages WHERE t.id = :id")
    Optional<Therapist> findWithLanguagesById(@Param("id") UUID id);
}
