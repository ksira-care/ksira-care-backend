package com.ksiracare.backend.dto.response;

/** The client as listed in the admin console. The email identifies them. */
public record ClientDto(String name, String email) {
}
