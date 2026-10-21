package com.ksiracare.backend.security;

import java.util.UUID;

/**
 * Whoever is signed in for the current request, taken from the session cookie — never from the
 * URL. Controllers ask for the concrete type they serve, e.g.
 * {@code @AuthenticationPrincipal AdminPrincipal me}.
 */
public sealed interface SessionPrincipal permits AaptaPrincipal, AdminPrincipal {

    UUID accountId();

    String email();

    Role role();

    static SessionPrincipal of(Role role, UUID accountId, String email) {
        return switch (role) {
            case AAPTA -> new AaptaPrincipal(accountId, email);
            case ADMIN -> new AdminPrincipal(accountId, email);
        };
    }
}
