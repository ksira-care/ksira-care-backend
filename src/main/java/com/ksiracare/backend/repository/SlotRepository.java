package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Slot;
import com.ksiracare.backend.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    /** An aapta's slots in [from, to), stored UTC. */
    @Query("""
            SELECT s FROM Slot s
            WHERE s.aapta.id = :aaptaId AND s.slotTime >= :from AND s.slotTime < :to
            ORDER BY s.slotTime ASC
            """)
    List<Slot> findForAapta(
            @Param("aaptaId") UUID aaptaId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Open hours starting exactly at {@code slotTime}, of active aaptas, by name. */
    @Query("""
            SELECT s FROM Slot s JOIN FETCH s.aapta a
            WHERE s.slotTime = :slotTime AND s.status = :status AND a.active = TRUE
            ORDER BY a.firstName ASC, a.lastName ASC
            """)
    List<Slot> findAtTimeWithStatus(@Param("slotTime") LocalDateTime slotTime, @Param("status") SlotStatus status);

    @Query("SELECT s FROM Slot s WHERE s.aapta.id = :aaptaId AND s.slotTime = :slotTime")
    Optional<Slot> findForAaptaAt(@Param("aaptaId") UUID aaptaId, @Param("slotTime") LocalDateTime slotTime);

    /** Every aapta's recorded hours in [from, to), with the aapta loaded. */
    @Query("""
            SELECT s FROM Slot s JOIN FETCH s.aapta
            WHERE s.slotTime >= :from AND s.slotTime < :to
            """)
    List<Slot> findStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** An aapta's hours with the given status that start after {@code after}. */
    @Query("SELECT s FROM Slot s WHERE s.aapta.id = :aaptaId AND s.status = :status AND s.slotTime > :after")
    List<Slot> findForAaptaWithStatusStartingAfter(@Param("aaptaId") UUID aaptaId,
                                                   @Param("status") SlotStatus status,
                                                   @Param("after") LocalDateTime after);

    /** Retention: deletes a period's hours that no booking points at (delete the bookings first). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            DELETE FROM slots
            WHERE slot_time >= :from AND slot_time < :to
              AND id NOT IN (SELECT slot_id FROM bookings WHERE slot_id IS NOT NULL)
            """)
    int deleteUnbookedStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
