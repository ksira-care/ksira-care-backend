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
                @Index(name = "idx_booking_aapta_start", columnList = "aapta_id, start_time"),
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

    /** Who holds the session; null until an admin allocates one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aapta_id")
    private Aapta aapta;

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

    /** When the client accepted the terms on the booking form. */
    @Column(name = "consent_given_at")
    private LocalDateTime consentGivenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "aapta_fee", precision = 10, scale = 2)
    private BigDecimal aaptaFee;

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

    /** Set by an admin once the client and aapta have been emailed the Meet link (BCC). */
    @Column(name = "bcc_email_sent", nullable = false)
    @Builder.Default
    private boolean bccEmailSent = false;

    /** When the aapta marked the session complete / client no-show; null while pending. */
    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Gives the session to the aapta who owns {@code slot}: that hour becomes booked, the previous
     * aapta's hour (if any) opens again, and any outcome is cleared — the new aapta marks it.
     * The BCC flag is left alone: it records whether the client was emailed. Callers check that
     * the slot is open and the change is allowed.
     */
    public void allocateTo(Slot slot, LocalDateTime now) {
        if (this.slot != null) {
            this.slot.release();
        }
        slot.book();
        this.slot = slot;
        this.aapta = slot.getAapta();
        this.assignedAt = now;
        this.status = BookingStatus.PENDING;
        this.markedAt = null;
    }

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
