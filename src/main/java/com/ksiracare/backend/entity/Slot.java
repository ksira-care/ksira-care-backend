package com.ksiracare.backend.entity;

import com.ksiracare.backend.enums.SlotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "slots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_aapta_slot_time", columnNames = {"aapta_id", "slot_time"})
        },
        indexes = {
                @Index(name = "idx_slots_aapta_time", columnList = "aapta_id, slot_time")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aapta_id", nullable = false)
    private Aapta aapta;

    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Slot(Aapta aapta, LocalDateTime slotTime, SlotStatus status) {
        this.aapta = aapta;
        this.slotTime = slotTime;
        this.status = status;
    }

    /** A booking now holds this hour. */
    public void book() {
        this.status = SlotStatus.BOOKED;
    }

    /** The aapta won't be working this hour. */
    public void close() {
        this.status = SlotStatus.AAPTA_UNAVAILABLE;
    }

    /** The booking moved elsewhere; the aapta's hour is open again. */
    public void release() {
        this.status = SlotStatus.AAPTA_AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Slot slot)) return false;
        return id != null && Objects.equals(id, slot.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
