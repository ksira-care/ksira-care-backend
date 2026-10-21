package com.ksiracare.backend.entity;

import com.ksiracare.backend.enums.Language;
import jakarta.persistence.*;
import lombok.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "languages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LanguageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true)
    private Language code;

    @Column(name = "name", nullable = false)
    private String name;

    public LanguageEntity(Language code, String name) {
        this.code = code;
        this.name = name;
    }

    /** The codes of {@code languages}, e.g. {HINDI, ENGLISH}. */
    public static Set<Language> codes(Collection<LanguageEntity> languages) {
        return languages.stream().map(LanguageEntity::getCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Language.class)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LanguageEntity that)) return false;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
