package com.ksiracare.backend.security;

/**
 * Who a session belongs to. Each role signs in through its own endpoints, carries its own
 * session cookie and may only call its own part of the API (see SecurityConfig).
 */
public enum Role {
    AAPTA,
    ADMIN;

    /** The Spring Security authority, e.g. "ROLE_ADMIN", as checked by {@code hasRole("ADMIN")}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
