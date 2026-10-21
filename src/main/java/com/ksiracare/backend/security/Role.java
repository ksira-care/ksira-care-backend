package com.ksiracare.backend.security;

/**
 * What kind of account a session belongs to. Everyone signs in at the same endpoint; the role is
 * carried in the session token and decides which part of the API it opens (see SecurityConfig).
 */
public enum Role {
    AAPTA,
    ADMIN;

    /** The Spring Security authority, e.g. "ROLE_ADMIN", as checked by {@code hasRole("ADMIN")}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
