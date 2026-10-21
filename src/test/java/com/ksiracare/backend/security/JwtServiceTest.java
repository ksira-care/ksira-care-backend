package com.ksiracare.backend.security;

import com.ksiracare.backend.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("a-test-signing-key-that-is-long-enough!!".getBytes());
    private static final Instant ISSUED = Instant.parse("2026-10-03T10:00:00Z");

    private static JwtService serviceAt(Instant now, String secret) {
        return new JwtService(new AuthProperties(Duration.ofHours(2), secret,
                new AuthProperties.Cookie("c", "/api", "Strict", true),
                new AuthProperties.LoginAttempts(5, Duration.ofMinutes(15))), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void aTokenIdentifiesItsAapta() {
        UUID id = UUID.randomUUID();
        String token = serviceAt(ISSUED, SECRET).issue(id, "aanya@ksiracare.com");

        assertThat(serviceAt(ISSUED.plusSeconds(60), SECRET).verify(token))
                .contains(new AaptaPrincipal(id, "aanya@ksiracare.com"));
    }

    @Test
    void anExpiredTokenIsRejected() {
        String token = serviceAt(ISSUED, SECRET).issue(UUID.randomUUID(), "aanya@ksiracare.com");

        assertThat(serviceAt(ISSUED.plus(Duration.ofHours(2)).plusSeconds(1), SECRET).verify(token)).isEmpty();
    }

    @Test
    void aTamperedOrForeignTokenIsRejected() {
        String token = serviceAt(ISSUED, SECRET).issue(UUID.randomUUID(), "aanya@ksiracare.com");
        String otherSecret = Base64.getEncoder().encodeToString("another-signing-key-that-is-long-enough!".getBytes());

        assertThat(serviceAt(ISSUED, otherSecret).verify(token)).isEmpty();
        assertThat(serviceAt(ISSUED, SECRET).verify(token + "x")).isEmpty();
        assertThat(serviceAt(ISSUED, SECRET).verify("not-a-token")).isEmpty();
    }

    @Test
    void aValidlySignedTokenWithoutTheAaptaClaimIsRejected() {
        // e.g. a session issued before "therapistId" was renamed to "aaptaId"
        String legacy = Jwts.builder()
                .subject("aanya@ksiracare.com")
                .claim("therapistId", UUID.randomUUID().toString())
                .issuedAt(Date.from(ISSUED))
                .expiration(Date.from(ISSUED.plus(Duration.ofHours(2))))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();

        assertThat(serviceAt(ISSUED, SECRET).verify(legacy)).isEmpty();
    }

    @Test
    void refusesToStartWithAWeakKey() {
        String weak = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> serviceAt(ISSUED, weak)).isInstanceOf(IllegalStateException.class);
    }
}
