package com.ksiracare.backend.security;

import com.ksiracare.backend.entity.Aapta;
import com.ksiracare.backend.repository.AaptaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Signs the request in from the session cookie. A missing, invalid or expired token — or an
 * account that has since been deactivated — simply leaves the request anonymous; protected
 * endpoints then answer 401. Registered only in the security chain (see SecurityConfig).
 */
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionCookies sessionCookies;
    private final JwtService jwtService;
    private final AaptaRepository aaptaRepository;

    public SessionAuthenticationFilter(SessionCookies sessionCookies,
                                       JwtService jwtService,
                                       AaptaRepository aaptaRepository) {
        this.sessionCookies = sessionCookies;
        this.jwtService = jwtService;
        this.aaptaRepository = aaptaRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        sessionCookies.read(request)
                .flatMap(jwtService::verify)
                .filter(this::isStillActive)
                .ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())));
        chain.doFilter(request, response);
    }

    /** Deactivating an account takes effect immediately, not when its session expires. */
    private boolean isStillActive(AaptaPrincipal principal) {
        return aaptaRepository.findById(principal.aaptaId())
                .map(Aapta::isActive)
                .orElse(false);
    }
}
