package com.ksiracare.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** A listener who holds sessions with clients. */
@Entity
@Table(name = "aaptas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Aapta extends Account {

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Must be a valid E.164 phone number")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "aapta_languages",
            joinColumns = @JoinColumn(name = "aapta_id"),
            inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    private Set<LanguageEntity> languages = new HashSet<>();

    private String address;

    /** Aaptas are created by admins; {@code passwordHash} must already be BCrypt-encoded. */
    public Aapta(String firstName, String lastName, String email, String passwordHash) {
        super(firstName, lastName, email, passwordHash);
    }
}
