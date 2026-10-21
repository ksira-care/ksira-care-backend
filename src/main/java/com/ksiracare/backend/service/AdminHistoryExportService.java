package com.ksiracare.backend.service;

import com.ksiracare.backend.entity.Booking;
import com.ksiracare.backend.entity.HistoryExport;
import com.ksiracare.backend.export.HistoryCsv;
import com.ksiracare.backend.policy.HistoryWindow;
import com.ksiracare.backend.repository.HistoryExportRepository;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * "Export CSV": always last month's bookings. Each export is logged, and months older than
 * History's window that were exported before are then deleted (see {@link HistoryRetention}).
 */
@Service
@RequiredArgsConstructor
public class AdminHistoryExportService {

    private final AdminHistoryService historyService;
    private final BookingHistory history;
    private final HistoryCsv historyCsv;
    private final HistoryExportRepository exportRepository;
    private final HistoryRetention retention;
    private final HistoryWindow window;
    private final PortalCalendar calendar;

    /**
     * @param month       the month in the file
     * @param csv         the file
     * @param deleted     older months deleted after this export
     * @param notExported older months kept because they were never exported
     */
    public record Export(YearMonth month, byte[] csv, List<YearMonth> deleted, List<YearMonth> notExported) {

        public String filename() {
            return "ksira-sessions-" + month + ".csv";
        }
    }

    @Transactional
    public Export export(AdminPrincipal admin) {
        YearMonth month = window.exportMonth();
        List<Booking> bookings = historyService.bookingsIn(month);
        byte[] csv = historyCsv.write(bookings, history.summarise(bookings.stream().map(Booking::getId).toList()));

        exportRepository.save(new HistoryExport(month, admin.adminId(), calendar.nowUtc()));
        HistoryRetention.Outcome retained = retention.deleteExportedMonths();
        return new Export(month, csv, retained.deleted(), retained.notExported());
    }
}
