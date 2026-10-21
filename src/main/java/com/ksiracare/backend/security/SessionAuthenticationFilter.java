package com.ksiracare.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Signs the request in from one role's session cookie. A missing, invalid or expired token, a
 * token issued for another role, an account that has since been deactivated, or a session the
 * account has since ended (e.g. by changing its password) simply leaves the request anonymous;
 * protected endpoints then answer 401. One instance per security chain
 * (see SecurityConfig), never registered as a servlet filter.
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final Role role;
    private final SessionCookies sessionCookies;
    private final JwtService jwtService;
    private final AccountDirectory accounts;

    public SessionAuthenticationFilter(Role role,
                                       SessionCookies sessionCookies,
                                       JwtService jwtService,
                                       AccountDirectory accounts) {
        this.role = role;
        this.sessionCookies = sessionCookies;
        this.jwtService = jwtService;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        sessionCookies.read(role, request)
                .flatMap(jwtService::verify)
                .filter(session -> session.principal().role() == role)
                .filter(this::isCurrent)
                .ifPresent(session -> SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                session.principal(), null, List.of(new SimpleGrantedAuthority(role.authority())))));
        chain.doFilter(request, response);
    }

    /** Deactivating an account, or ending its sessions, takes effect immediately — not when the token expires. */
    private boolean isCurrent(JwtService.VerifiedSession session) {
        SessionPrincipal principal = session.principal();
        return accounts.isSessionCurrent(principal.role(), principal.accountId(), session.sessionVersion());
    }
}
