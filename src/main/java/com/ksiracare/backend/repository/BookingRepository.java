package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** All ranges are start-inclusive, end-exclusive, in stored UTC. */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** An aapta's bookings starting in [from, to), excluding the given statuses, with languages loaded. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.aapta.id = :aaptaId
              AND b.startTime >= :from AND b.startTime < :to
              AND b.status NOT IN :excludedStatuses
            ORDER BY b.startTime ASC
            """)
    List<Booking> findForAapta(
            @Param("aaptaId") UUID aaptaId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludedStatuses") Collection<BookingStatus> excludedStatuses
    );

    /** Only finds the booking if it belongs to this aapta. */
    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.id = :bookingId AND b.aapta.id = :aaptaId
            """)
    Optional<Booking> findOwnedBy(@Param("bookingId") UUID bookingId, @Param("aaptaId") UUID aaptaId);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.aapta.id = :aaptaId AND b.status = :status
              AND b.startTime >= :from AND b.startTime < :to
            """)
    long countWithStatusBetween(
            @Param("aaptaId") UUID aaptaId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.aapta.id = :aaptaId AND b.status = :status")
    long countWithStatus(@Param("aaptaId") UUID aaptaId, @Param("status") BookingStatus status);

    @Query("SELECT MIN(b.startTime) FROM Booking b WHERE b.aapta.id = :aaptaId AND b.status = :status")
    Optional<LocalDateTime> findEarliestStartWithStatus(
            @Param("aaptaId") UUID aaptaId,
            @Param("status") BookingStatus status
    );

    /** Every booking starting in [from, to), whoever holds it, with its aapta and client languages loaded. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.aapta
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.startTime >= :from AND b.startTime < :to
              AND b.status NOT IN :excludedStatuses
            ORDER BY b.startTime ASC
            """)
    List<Booking> findStartingBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludedStatuses") Collection<BookingStatus> excludedStatuses
    );

    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.aapta
            LEFT JOIN FETCH b.slot
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.id = :bookingId
            """)
    Optional<Booking> findWithDetailsById(@Param("bookingId") UUID bookingId);
}
