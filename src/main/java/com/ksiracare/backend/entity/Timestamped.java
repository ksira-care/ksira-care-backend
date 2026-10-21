package com.ksiracare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * When a row was created and last changed, in UTC like every other stored time. Stamped here
 * rather than with Hibernate's @CreationTimestamp, which uses the JVM's time zone and so would be
 * off by the host's offset (e.g. +05:30 on a laptop in India).
 */
@MappedSuperclass
@Getter
public abstract class Timestamped {

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void stampCreated() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    void stampUpdated() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
