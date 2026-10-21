package com.ksiracare.backend.repository;

import com.ksiracare.backend.entity.HistoryExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface HistoryExportRepository extends JpaRepository<HistoryExport, UUID> {

    /** First days of every month that has been exported at least once. */
    @Query("SELECT DISTINCT e.month FROM HistoryExport e")
    Set<LocalDate> findExportedMonths();
}
