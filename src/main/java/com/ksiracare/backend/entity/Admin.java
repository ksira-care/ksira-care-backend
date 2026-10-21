package com.ksiracare.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Ksira Care staff who manage aaptas and bookings. */
@Entity
@Table(name = "admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin extends Account {

    /** {@code passwordHash} must already be BCrypt-encoded. */
    public Admin(String firstName, String lastName, String email, String passwordHash) {
        super(firstName, lastName, email, passwordHash);
    }
}
