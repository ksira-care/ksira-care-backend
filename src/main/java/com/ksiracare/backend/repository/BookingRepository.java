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

    /** A therapist's bookings starting in [from, to), excluding the given statuses, with languages loaded. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.therapist.id = :therapistId
              AND b.startTime >= :from AND b.startTime < :to
              AND b.status NOT IN :excludedStatuses
            ORDER BY b.startTime ASC
            """)
    List<Booking> findForTherapist(
            @Param("therapistId") UUID therapistId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("excludedStatuses") Collection<BookingStatus> excludedStatuses
    );

    /** Only finds the booking if it belongs to this therapist. */
    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.id = :bookingId AND b.therapist.id = :therapistId
            """)
    Optional<Booking> findOwnedBy(@Param("bookingId") UUID bookingId, @Param("therapistId") UUID therapistId);

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.therapist.id = :therapistId AND b.status = :status
              AND b.startTime >= :from AND b.startTime < :to
            """)
    long countWithStatusBetween(
            @Param("therapistId") UUID therapistId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.therapist.id = :therapistId AND b.status = :status")
    long countWithStatus(@Param("therapistId") UUID therapistId, @Param("status") BookingStatus status);

    @Query("SELECT MIN(b.startTime) FROM Booking b WHERE b.therapist.id = :therapistId AND b.status = :status")
    Optional<LocalDateTime> findEarliestStartWithStatus(
            @Param("therapistId") UUID therapistId,
            @Param("status") BookingStatus status
    );
}
