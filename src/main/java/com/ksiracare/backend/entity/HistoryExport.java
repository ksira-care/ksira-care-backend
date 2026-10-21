package com.ksiracare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/** A record that an admin exported one month of History. */
@Entity
@Table(name = "history_exports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoryExport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** First day of the exported month. */
    @Column(name = "export_month", nullable = false)
    private LocalDate month;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "exported_at", nullable = false)
    private LocalDateTime exportedAt;

    public HistoryExport(YearMonth month, UUID adminId, LocalDateTime exportedAt) {
        this.month = month.atDay(1);
        this.adminId = adminId;
        this.exportedAt = exportedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HistoryExport export)) return false;
        return id != null && Objects.equals(id, export.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
