package com.ksiracare.backend.security;

import java.util.UUID;

/**
 * The signed-in aapta for the current request. Controllers receive it via
 * {@code @AuthenticationPrincipal} — the aapta is always taken from the session,
 * never from the URL, so nobody can act on another aapta's data.
 */
public record AaptaPrincipal(UUID aaptaId, String email) {
}
