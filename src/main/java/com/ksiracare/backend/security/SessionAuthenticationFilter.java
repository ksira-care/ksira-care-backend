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
 * Signs the request in from the session cookie, with the authority of the role the token was
 * issued for. A missing, invalid or expired token, an account that has since been deactivated,
 * or a session the account has since ended (e.g. by changing its password) simply leaves the
 * request anonymous; protected endpoints then answer 401. Never registered as a servlet filter
 * (see SecurityConfig).
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionCookies sessionCookies;
    private final JwtService jwtService;
    private final AccountDirectory accounts;

    public SessionAuthenticationFilter(SessionCookies sessionCookies, JwtService jwtService, AccountDirectory accounts) {
        this.sessionCookies = sessionCookies;
        this.jwtService = jwtService;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        sessionCookies.read(request)
                .flatMap(jwtService::verify)
                .filter(this::isCurrent)
                .map(JwtService.VerifiedSession::principal)
                .ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, List.of(new SimpleGrantedAuthority(principal.role().authority())))));
        chain.doFilter(request, response);
    }

    /** Deactivating an account, or ending its sessions, takes effect immediately — not when the token expires. */
    private boolean isCurrent(JwtService.VerifiedSession session) {
        SessionPrincipal principal = session.principal();
        return accounts.isSessionCurrent(principal.role(), principal.accountId(), session.sessionVersion());
    }
}
