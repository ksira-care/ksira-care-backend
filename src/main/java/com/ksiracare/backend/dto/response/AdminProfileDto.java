package com.ksiracare.backend.dto.response;

import java.util.UUID;

/** The signed-in admin, e.g. for the console header. */
public record AdminProfileDto(UUID adminId, String email, String firstName, String middleName, String lastName) {
}
