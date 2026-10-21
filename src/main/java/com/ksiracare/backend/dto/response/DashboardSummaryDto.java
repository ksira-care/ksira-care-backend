package com.ksiracare.backend.dto.response;

/**
 * Headline numbers for the dashboard, both in the portal time zone (IST). Only recent numbers:
 * older months are deleted once exported, so an all-time total would quietly shrink.
 *
 * @param completedThisMonth  sessions completed in the current calendar month
 * @param completedLast30Days sessions completed from 30 days ago up to today, inclusive
 */
public record DashboardSummaryDto(long completedThisMonth, long completedLast30Days) {
}
