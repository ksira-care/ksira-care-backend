package com.ksiracare.backend.dto.response;

import com.ksiracare.backend.entity.Account;
import com.ksiracare.backend.security.Role;

import java.util.UUID;

/**
 * Who is signed in, and as what: {@code role} tells the app which portal to open, and the name
 * is for its header. The session itself travels in the httpOnly cookie, never in the body.
 */
public record SessionDto(UUID accountId, String email, Role role, String firstName, String middleName, String lastName) {

    public static SessionDto of(Role role, Account account) {
        return new SessionDto(account.getId(), account.getEmail(), role,
                account.getFirstName(), account.getMiddleName(), account.getLastName());
    }
}
