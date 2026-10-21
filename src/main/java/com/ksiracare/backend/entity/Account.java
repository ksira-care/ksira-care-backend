package com.ksiracare.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Someone who signs in with an email and password: an aapta or an admin. Each kind has its own
 * table; this holds the columns they share. Accounts are created by admins, so there is no sign-up.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Account extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "First name is required")
    @Column(nullable = false)
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    @Column(nullable = false)
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the plain password. */
    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    /** Deactivated accounts can't sign in, and their open sessions stop working. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Sessions are only accepted if issued under this version; see {@link #endAllSessions()}. */
    @Setter(AccessLevel.NONE)
    @Column(name = "session_version", nullable = false)
    private int sessionVersion;

    /** {@code passwordHash} must already be BCrypt-encoded. */
    protected Account(String firstName, String lastName, String email, String passwordHash) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = passwordHash;
    }

    /** How emails are stored: trimmed and lower-case, so uniqueness and sign-in ignore case. */
    public static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Signs the account out everywhere: every session issued before now stops working. */
    public void endAllSessions() {
        sessionVersion++;
    }

    /** Sets a new password (already BCrypt-encoded) and signs the account out everywhere. */
    public void changePassword(String passwordHash) {
        this.password = passwordHash;
        endAllSessions();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other) || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
