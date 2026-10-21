package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.LanguageEntity;
import com.ksiracare.backend.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<LanguageEntity, Integer> {
    Optional<LanguageEntity> findByCode(Language code);

    List<LanguageEntity> findAllByCodeIn(Collection<Language> codes);

    List<LanguageEntity> findAllByOrderByNameAsc();
}
