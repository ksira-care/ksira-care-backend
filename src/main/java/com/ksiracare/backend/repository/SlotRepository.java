package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    @Query("SELECT s FROM Slot s WHERE s.therapist.id = :therapistId AND s.slotTime >= :startTime AND s.slotTime <= :endTime ORDER BY s.slotTime ASC")
    List<Slot> findByTherapistIdAndSlotTimeBetween(
            @Param("therapistId") UUID therapistId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    Optional<Slot> findByTherapistIdAndSlotTime(UUID therapistId, LocalDateTime slotTime);
}
