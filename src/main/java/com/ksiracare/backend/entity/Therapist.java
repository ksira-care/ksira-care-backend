package com.ksiracare.backend.entity;

import com.ksiracare.backend.enums.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "therapists")
@Getter
@Setter
public class Therapist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    private String middleName;

    private String lastName;

    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private LocalDate dateOfBirth;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "preferred_languages",
            joinColumns = @JoinColumn(name = "therapist_id")
    )
    @Column(name = "language")
    private Set<Language> languages = new HashSet<>();

    private String address;
}
