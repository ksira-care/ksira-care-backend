package com.ksiracare.backend.dto.response;

import java.time.LocalDate;

/**
 * Headline numbers for the dashboard. "This month" is the current calendar month in the
 * portal time zone (IST).
 *
 * @param activeSince date of the first completed session, or null if there isn't one yet
 */
public record DashboardSummaryDto(long completedThisMonth, long completedAllTime, LocalDate activeSince) {
}
