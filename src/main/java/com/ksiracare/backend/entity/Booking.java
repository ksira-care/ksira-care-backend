package com.ksiracare.backend.entity;

import com.ksiracare.backend.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_therapist_start", columnList = "therapist_id, start_time"),
                @Index(name = "idx_booking_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapist_id", nullable = false)
    private Therapist therapist;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private Slot slot;

    // Direct Embedded Customer Data
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_country")
    private String customerCountry;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_customer_languages",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    @Builder.Default
    private Set<LanguageEntity> customerPreferredLanguages = new HashSet<>();

    // Session Timestamps
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "booking_reason", length = 1000)
    private String bookingReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "therapist_fee", precision = 10, scale = 2)
    private BigDecimal therapistFee;

    // Structured Audit Fields
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "is_rescheduled", nullable = false)
    @Builder.Default
    private boolean rescheduled = false;

    @Column(name = "previous_start_time")
    private LocalDateTime previousStartTime;

    @Column(name = "reschedule_reason")
    private String rescheduleReason;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking booking)) return false;
        return id != null && Objects.equals(id, booking.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
