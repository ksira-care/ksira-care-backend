package com.ksiracare.backend.entity;

import com.ksiracare.backend.enums.BookingEventType;
import com.ksiracare.backend.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** One change to a booking: what happened, who did it and when. Append-only. */
@Entity
@Table(name = "booking_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingEventType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false)
    private Role actorRole;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    /** For allocation events: who had the booking before (null on first allocation) and after. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_aapta_id")
    private Aapta previousAapta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aapta_id")
    private Aapta aapta;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** Free text where the change needs explaining, e.g. why a booking was cancelled. */
    @Column(length = 500)
    private String note;

    @Builder
    private BookingEvent(Booking booking, BookingEventType type, Role actorRole, UUID actorId,
                         Aapta previousAapta, Aapta aapta, LocalDateTime occurredAt, String note) {
        this.booking = booking;
        this.type = type;
        this.actorRole = actorRole;
        this.actorId = actorId;
        this.previousAapta = previousAapta;
        this.aapta = aapta;
        this.occurredAt = occurredAt;
        this.note = note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingEvent event)) return false;
        return id != null && Objects.equals(id, event.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
