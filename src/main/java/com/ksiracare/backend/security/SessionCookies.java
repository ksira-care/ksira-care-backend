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
 * Sessions live in httpOnly cookies: page scripts can't read them, so injected scripts can't
 * steal them. SameSite=Strict keeps other sites from sending them (CSRF). Each {@link Role} has
 * its own cookie name and path.
 */
@Component
public class SessionCookies {

    private final AuthProperties.Cookie settings;
    private final Duration maxAge;

    public SessionCookies(AuthProperties properties) {
        this.settings = properties.cookie();
        this.maxAge = properties.sessionTtl();
    }

    /** A cookie carrying a session token, living as long as the token does. */
    public ResponseCookie issue(Role role, String token) {
        return base(role, token).maxAge(maxAge).build();
    }

    /** Tells the browser to delete the cookie (same name and path, zero lifetime). */
    public ResponseCookie clear(Role role) {
        return base(role, "").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(Role role, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String name = settings.of(role).name();
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(Role role, String value) {
        AuthProperties.RoleCookie cookie = settings.of(role);
        return ResponseCookie.from(cookie.name(), value)
                .httpOnly(true)
                .secure(settings.secure())
                .sameSite(settings.sameSite())
                .path(cookie.path());
    }
}
