package com.ksiracare.backend.dto.response;

import java.util.List;

/** Hours a client can book, in order. Times are epoch milliseconds. */
public record BookableHoursDto(List<Hour> hours) {

    public record Hour(Long startTime, Long endTime) {
    }
}
