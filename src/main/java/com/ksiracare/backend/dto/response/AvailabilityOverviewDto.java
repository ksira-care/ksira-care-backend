package com.ksiracare.backend.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Every aapta's day, hour by hour, for the admin console's availability grid (view only).
 * Each aapta's {@code slots} line up with {@code hours}, column for column.
 */
public record AvailabilityOverviewDto(LocalDate date, List<HourDto> hours, List<AaptaAvailabilityDto> aaptas) {

    /**
     * One column of the grid.
     *
     * @param time       session start, epoch milliseconds (UTC)
     * @param openAaptas how many aaptas are free to book at this hour (the footer row)
     */
    public record HourDto(Long time, int openAaptas) {
    }
}
