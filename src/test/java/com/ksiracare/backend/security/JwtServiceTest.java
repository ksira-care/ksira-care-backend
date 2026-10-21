package com.ksiracare.backend.security;

import com.ksiracare.backend.support.TestAuth;
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
        return new JwtService(TestAuth.properties(secret), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void aTokenIdentifiesItsAaptaAndRole() {
        AaptaPrincipal aapta = new AaptaPrincipal(UUID.randomUUID(), "aanya@ksiracare.com");
        String token = serviceAt(ISSUED, SECRET).issue(aapta, 3);

        assertThat(serviceAt(ISSUED.plusSeconds(60), SECRET).verify(token))
                .contains(new JwtService.VerifiedSession(aapta, 3));
    }

    @Test
    void aTokenIdentifiesItsAdminAndRole() {
        AdminPrincipal admin = new AdminPrincipal(UUID.randomUUID(), "meera@ksiracare.com");
        String token = serviceAt(ISSUED, SECRET).issue(admin, 0);

        assertThat(serviceAt(ISSUED.plusSeconds(60), SECRET).verify(token))
                .contains(new JwtService.VerifiedSession(admin, 0));
    }

    @Test
    void anExpiredTokenIsRejected() {
        String token = serviceAt(ISSUED, SECRET).issue(new AaptaPrincipal(UUID.randomUUID(), "aanya@ksiracare.com"), 0);

        assertThat(serviceAt(ISSUED.plus(Duration.ofHours(2)).plusSeconds(1), SECRET).verify(token)).isEmpty();
    }

    @Test
    void aTamperedOrForeignTokenIsRejected() {
        String token = serviceAt(ISSUED, SECRET).issue(new AaptaPrincipal(UUID.randomUUID(), "aanya@ksiracare.com"), 0);
        String otherSecret = Base64.getEncoder().encodeToString("another-signing-key-that-is-long-enough!".getBytes());

        assertThat(serviceAt(ISSUED, otherSecret).verify(token)).isEmpty();
        assertThat(serviceAt(ISSUED, SECRET).verify(token + "x")).isEmpty();
        assertThat(serviceAt(ISSUED, SECRET).verify("not-a-token")).isEmpty();
    }

    @Test
    void aValidlySignedTokenInAnOlderFormatIsRejected() {
        // Sessions issued before roles existed: {"sub": email, "therapistId": ...}, no role or version.
        String legacy = signed(Jwts.builder()
                .subject("aanya@ksiracare.com")
                .claim("therapistId", UUID.randomUUID().toString()));
        String unknownRole = signed(Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "SUPERUSER")
                .claim("sv", 0));
        String noSessionVersion = signed(Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "AAPTA"));

        assertThat(serviceAt(ISSUED, SECRET).verify(legacy)).isEmpty();
        assertThat(serviceAt(ISSUED, SECRET).verify(unknownRole)).isEmpty();
        assertThat(serviceAt(ISSUED, SECRET).verify(noSessionVersion)).isEmpty();
    }

    @Test
    void refusesToStartWithAWeakKey() {
        String weak = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> serviceAt(ISSUED, weak)).isInstanceOf(IllegalStateException.class);
    }

    private static String signed(io.jsonwebtoken.JwtBuilder builder) {
        return builder
                .issuedAt(Date.from(ISSUED))
                .expiration(Date.from(ISSUED.plus(Duration.ofHours(2))))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .compact();
    }
}
