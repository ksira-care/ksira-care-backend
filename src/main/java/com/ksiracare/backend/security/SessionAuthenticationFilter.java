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
 * token issued for another role, or an account that has since been deactivated simply leaves
 * the request anonymous; protected endpoints then answer 401. One instance per security chain
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
                .filter(principal -> principal.role() == role)
                .filter(this::isStillActive)
                .ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, List.of(new SimpleGrantedAuthority(role.authority())))));
        chain.doFilter(request, response);
    }

    /** Deactivating an account takes effect immediately, not when its session expires. */
    private boolean isStillActive(SessionPrincipal principal) {
        return accounts.isActive(principal.role(), principal.accountId());
    }
}
