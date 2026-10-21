package com.ksiracare.backend.security;

import com.ksiracare.backend.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies the signed session token stored in the session cookie:
 * {@code sub} = account id, plus the account's {@code email}, {@code role} and the session
 * version ({@code sv}) it was issued under.
 */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String SESSION_VERSION_CLAIM = "sv";
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(AuthProperties properties, Clock clock) {
        this.signingKey = toKey(properties.jwtSecret());
        this.ttl = properties.sessionTtl();
        this.clock = clock;
    }

    /** A verified token: who it belongs to, and the session version it was issued under. */
    public record VerifiedSession(SessionPrincipal principal, int sessionVersion) {
    }

    public String issue(SessionPrincipal principal, int sessionVersion) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(principal.accountId().toString())
                .claim(EMAIL_CLAIM, principal.email())
                .claim(ROLE_CLAIM, principal.role().name())
                .claim(SESSION_VERSION_CLAIM, sessionVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Who the token belongs to, or empty if it's tampered with, malformed, expired, or from an
     * older format without a role or session version (those sessions simply end). Whether the
     * session version is still current is for the caller to check against the account.
     */
    public Optional<VerifiedSession> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get(ROLE_CLAIM, String.class);
            Integer sessionVersion = claims.get(SESSION_VERSION_CLAIM, Integer.class);
            if (role == null || sessionVersion == null || claims.getSubject() == null) {
                return Optional.empty();
            }
            SessionPrincipal principal = SessionPrincipal.of(
                    Role.valueOf(role), UUID.fromString(claims.getSubject()), claims.get(EMAIL_CLAIM, String.class));
            return Optional.of(new VerifiedSession(principal, sessionVersion));
        } catch (JwtException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    private static SecretKey toKey(String base64Secret) {
        byte[] bytes = Decoders.BASE64.decode(base64Secret);
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("ksira.auth.jwt-secret must decode to at least 256 bits");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
