package com.ksiracare.backend.controller;

import com.ksiracare.backend.dto.response.HistoryDto;
import com.ksiracare.backend.security.AdminPrincipal;
import com.ksiracare.backend.service.AdminHistoryExportService;
import com.ksiracare.backend.service.AdminHistoryService;
import com.ksiracare.backend.time.PortalCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/** The admin console's History: this month and last, and the export of last month. */
@RestController
@RequestMapping("/admin/history")
@RequiredArgsConstructor
public class AdminHistoryController {

    private final AdminHistoryService adminHistoryService;
    private final PortalCalendar calendar;

    private final AdminHistoryExportService exportService;

    /** One month, e.g. {@code ?month=2026-09}; the current month if none is given. */
    @GetMapping
    public HistoryDto getMonth(@RequestParam(required = false) YearMonth month) {
        return adminHistoryService.getMonth(month != null ? month : calendar.currentMonth());
    }

    /**
     * Downloads last month as CSV — whichever month is on screen — and then deletes older months
     * that were already exported. A POST, because it deletes data: nothing may trigger it by
     * merely fetching a URL. The months deleted, and any kept because they were never exported,
     * are listed in the {@code X-Deleted-Months} / {@code X-Not-Exported-Months} headers.
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AdminPrincipal me) {
        AdminHistoryExportService.Export export = exportService.export(me);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename()).build().toString())
                .header("X-Deleted-Months", months(export.deleted()))
                .header("X-Not-Exported-Months", months(export.notExported()))
                .body(export.csv());
    }

    private static String months(List<YearMonth> months) {
        return months.stream().map(YearMonth::toString).collect(Collectors.joining(","));
    }
}
