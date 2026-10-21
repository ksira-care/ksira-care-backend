package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.security.Role;
import com.ksiracare.backend.security.SessionPrincipal;

import java.util.UUID;

/**
 * Who is signed in, and as what: {@code role} tells the app which portal to open. The session
 * itself travels in the httpOnly cookie, never in the body.
 */
public record SessionDto(UUID accountId, String email, Role role) {

    public static SessionDto of(SessionPrincipal principal) {
        return new SessionDto(principal.accountId(), principal.email(), principal.role());
    }
}
