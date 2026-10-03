package com.ksiracare.backend.security;

import java.util.UUID;

/**
 * The signed-in therapist for the current request. Controllers receive it via
 * {@code @AuthenticationPrincipal} — the therapist is always taken from the session,
 * never from the URL, so nobody can act on another therapist's data.
 */
public record TherapistPrincipal(UUID therapistId, String email) {
}
