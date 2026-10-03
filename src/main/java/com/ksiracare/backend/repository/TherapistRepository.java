package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TherapistRepository extends JpaRepository<Therapist, Long> {
}
