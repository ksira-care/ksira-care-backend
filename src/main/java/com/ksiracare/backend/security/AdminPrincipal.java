package com.ksiracare.backend.security;

import java.util.UUID;

/** The signed-in admin. */
public record AdminPrincipal(UUID adminId, String email) implements SessionPrincipal {

    @Override
    public UUID accountId() {
        return adminId;
    }

    @Override
    public Role role() {
        return Role.ADMIN;
    }
}
