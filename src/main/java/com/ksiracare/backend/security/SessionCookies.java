package com.ksiracare.backend.security;

import com.ksiracare.backend.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * The session lives in an httpOnly cookie: page scripts can't read it, so injected
 * scripts can't steal it. SameSite=Strict keeps other sites from sending it (CSRF).
 */
@Component
public class SessionCookies {

    private final AuthProperties.Cookie settings;

    public SessionCookies(AuthProperties properties) {
        this.settings = properties.cookie();
    }

    public ResponseCookie issue(String token, Duration maxAge) {
        return base(token).maxAge(maxAge).build();
    }

    /** Tells the browser to delete the cookie (same name and path, zero lifetime). */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> settings.name().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(settings.name(), value)
                .httpOnly(true)
                .secure(settings.secure())
                .sameSite(settings.sameSite())
                .path(settings.path());
    }
}
