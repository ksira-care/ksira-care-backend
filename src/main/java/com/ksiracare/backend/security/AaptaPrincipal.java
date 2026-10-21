package com.ksiracare.backend.security;

import java.util.UUID;

/**
 * The signed-in aapta. The aapta is always taken from the session, never from the URL, so
 * nobody can act on another aapta's data.
 */
public record AaptaPrincipal(UUID aaptaId, String email) implements SessionPrincipal {

    @Override
    public UUID accountId() {
        return aaptaId;
    }

    @Override
    public Role role() {
        return Role.AAPTA;
    }
}
