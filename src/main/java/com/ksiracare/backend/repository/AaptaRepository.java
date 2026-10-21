package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Aapta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AaptaRepository extends AccountRepository<Aapta> {

    /** For the profile: loads languages in the same query. */
    @Query("SELECT a FROM Aapta a LEFT JOIN FETCH a.languages WHERE a.id = :id")
    Optional<Aapta> findWithLanguagesById(@Param("id") UUID id);

    List<Aapta> findAllByActiveTrue();
}
