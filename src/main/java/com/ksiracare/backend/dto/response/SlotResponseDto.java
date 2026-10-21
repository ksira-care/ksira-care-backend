package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.enums.SlotStatus;

import java.util.UUID;

/**
 * One session hour.
 *
 * @param id   null for an hour that has never been opened
 * @param time start of the hour, epoch milliseconds (UTC)
 */
public record SlotResponseDto(UUID id, Long time, SlotStatus status) {
}
