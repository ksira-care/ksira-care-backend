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

/** Issues and verifies the signed session token stored in the session cookie. */
@Service
public class JwtService {

    private static final String AAPTA_ID_CLAIM = "aaptaId";
    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(AuthProperties properties, Clock clock) {
        this.signingKey = toKey(properties.jwtSecret());
        this.ttl = properties.sessionTtl();
        this.clock = clock;
    }

    public String issue(UUID aaptaId, String email) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(email)
                .claim(AAPTA_ID_CLAIM, aaptaId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    /** The aapta the token belongs to, or empty if it's tampered with, malformed or expired. */
    public Optional<AaptaPrincipal> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // Missing on tokens issued before the claim was renamed; those sessions simply end.
            String aaptaId = claims.get(AAPTA_ID_CLAIM, String.class);
            if (aaptaId == null) {
                return Optional.empty();
            }
            return Optional.of(new AaptaPrincipal(UUID.fromString(aaptaId), claims.getSubject()));
        } catch (JwtException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public Duration ttl() {
        return ttl;
    }

    private static SecretKey toKey(String base64Secret) {
        byte[] bytes = Decoders.BASE64.decode(base64Secret);
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("ksira.auth.jwt-secret must decode to at least 256 bits");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
