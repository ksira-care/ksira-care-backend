package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b JOIN FETCH b.therapist WHERE b.therapist.id = :therapistId AND b.startTime >= :startTime AND b.startTime <= :endTime ORDER BY b.startTime ASC")
    List<Booking> findByTherapistIdAndStartTimeBetween(
            @Param("therapistId") UUID therapistId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.therapist.id = :therapistId AND b.startTime >= :startTime AND b.startTime <= :endTime")
    long countByTherapistIdAndStartTimeBetween(
            @Param("therapistId") UUID therapistId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.therapist.id = :therapistId AND b.status = :status")
    long countByTherapistIdAndStatus(
            @Param("therapistId") UUID therapistId,
            @Param("status") BookingStatus status
    );
}
