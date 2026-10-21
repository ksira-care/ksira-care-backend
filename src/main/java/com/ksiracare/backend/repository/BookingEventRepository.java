package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.BookingEvent;
import com.ksiracare.backend.enums.BookingEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingEventRepository extends JpaRepository<BookingEvent, UUID> {

    /** Events of one type for the given bookings, newest first, with the aaptas involved loaded. */
    @Query("""
            SELECT e FROM BookingEvent e
            LEFT JOIN FETCH e.previousAapta
            LEFT JOIN FETCH e.aapta
            WHERE e.booking.id IN :bookingIds AND e.type = :type
            ORDER BY e.occurredAt DESC
            """)
    List<BookingEvent> findForBookings(@Param("bookingIds") Collection<UUID> bookingIds,
                                       @Param("type") BookingEventType type);
}
