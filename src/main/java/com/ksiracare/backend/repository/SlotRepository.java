package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    /** A therapist's slots in [from, to), stored UTC. */
    @Query("""
            SELECT s FROM Slot s
            WHERE s.therapist.id = :therapistId AND s.slotTime >= :from AND s.slotTime < :to
            ORDER BY s.slotTime ASC
            """)
    List<Slot> findForTherapist(
            @Param("therapistId") UUID therapistId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
