package com.ksiracare.backend.config;

import com.ksiracare.backend.service.MarkingDeadlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the end-of-day marking deadline hourly and whenever the app starts. Hourly rather than
 * once at midnight, and on start-up too, because free hosting puts the app to sleep when idle
 * and a midnight-only job would often be missed; the check is idempotent.
 */
@Component
@RequiredArgsConstructor
public class MarkingDeadlineScheduler {

    private final MarkingDeadlineService markingDeadlineService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        markingDeadlineService.recordMissedMarks();
    }

    @Scheduled(cron = "${ksira.portal.marking-deadline-cron}", zone = "${ksira.portal.time-zone}")
    public void hourly() {
        markingDeadlineService.recordMissedMarks();
    }
}
