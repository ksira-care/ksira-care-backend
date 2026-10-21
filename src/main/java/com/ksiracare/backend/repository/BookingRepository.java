package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Bookings still waiting for an aapta, starting in [from, to), with the client's languages loaded. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.aapta IS NULL AND b.status = com.ksiracare.backend.enums.BookingStatus.PENDING
              AND b.startTime >= :from AND b.startTime < :to
            """)
    List<Booking> findUnallocatedStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Bookings still waiting for an aapta that start exactly at {@code start}, with the client's languages loaded. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.aapta IS NULL AND b.status = com.ksiracare.backend.enums.BookingStatus.PENDING
              AND b.startTime = :start
            """)
    List<Booking> findUnallocatedStartingAt(@Param("start") LocalDateTime start);

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

    /** Every booking starting in [from, to), whoever holds it and whatever its status, with its aapta and the client's languages loaded. */
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.aapta
            LEFT JOIN FETCH b.customerPreferredLanguages
            WHERE b.startTime >= :from AND b.startTime < :to
            ORDER BY b.startTime ASC
            """)
    List<Booking> findStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.aapta
            LEFT JOIN FETCH b.slot
            WHERE b.id = :bookingId
            """)
    Optional<Booking> findWithDetailsById(@Param("bookingId") UUID bookingId);

    /** Bookings in [from, to) that hold an aapta's hour, with that hour loaded. */
    @Query("""
            SELECT b FROM Booking b JOIN FETCH b.slot
            WHERE b.startTime >= :from AND b.startTime < :to
            """)
    List<Booking> findAllocatedStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** An aapta's bookings with the given status that start after {@code after}, with their hour loaded. */
    @Query("""
            SELECT b FROM Booking b LEFT JOIN FETCH b.slot
            WHERE b.aapta.id = :aaptaId AND b.status = :status AND b.startTime > :after
            """)
    List<Booking> findForAaptaWithStatusStartingAfter(@Param("aaptaId") UUID aaptaId,
                                                      @Param("status") BookingStatus status,
                                                      @Param("after") LocalDateTime after);

    /** Allocated sessions still waiting for their aapta's mark that started before {@code before}. */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = com.ksiracare.backend.enums.BookingStatus.PENDING
              AND b.aapta IS NOT NULL AND b.startTime < :before
            """)
    List<Booking> findUnmarkedAllocatedStartingBefore(@Param("before") LocalDateTime before);

    @Query("SELECT MIN(b.startTime) FROM Booking b")
    Optional<LocalDateTime> findEarliestStart();

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.startTime >= :from AND b.startTime < :to")
    long countStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // ── Retention: deleting a period's bookings with everything that hangs off them ──

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            DELETE FROM booking_events
            WHERE booking_id IN (SELECT id FROM bookings WHERE start_time >= :from AND start_time < :to)
            """)
    int deleteEventsOfBookingsStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            DELETE FROM booking_customer_languages
            WHERE booking_id IN (SELECT id FROM bookings WHERE start_time >= :from AND start_time < :to)
            """)
    int deleteLanguagesOfBookingsStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true, value = "DELETE FROM bookings WHERE start_time >= :from AND start_time < :to")
    int deleteStartingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
