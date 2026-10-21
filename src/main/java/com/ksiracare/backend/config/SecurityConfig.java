package com.ksiracare.backend.config;

import com.ksiracare.backend.exception.ErrorCode;
import com.ksiracare.backend.security.AccountDirectory;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.ProblemAuthenticationEntryPoint;
import com.ksiracare.backend.security.Role;
import com.ksiracare.backend.security.SessionAuthenticationFilter;
import com.ksiracare.backend.security.SessionCookies;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless, cookie-based security with one filter chain per {@link Role}:
 * <ul>
 *   <li>{@code /admin/**} — admin console; only the admin session cookie is read here.</li>
 *   <li>everything else — the aapta portal; only the aapta session cookie is read here.</li>
 * </ul>
 * A session for one role is therefore never accepted by the other's endpoints. Only sign-in and
 * sign-out are public. CSRF tokens aren't used: cookies are SameSite=Strict and the API only
 * accepts JSON, so other sites can't make authenticated requests.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionCookies sessionCookies;
    private final JwtService jwtService;
    private final AccountDirectory accounts;

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/admin/auth/login", "/admin/auth/logout").permitAll()
                        .anyRequest().hasRole(Role.ADMIN.name()));
        return withSession(http, Role.ADMIN).build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain aaptaSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Only reachable when springdoc is enabled (development).
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().hasRole(Role.AAPTA.name()));
        return withSession(http, Role.AAPTA).build();
    }

    /** What every chain shares: stateless, no browser login forms, problem-format errors, one role's cookie. */
    private HttpSecurity withSession(HttpSecurity http, Role role) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new ProblemAuthenticationEntryPoint(
                                ErrorCode.UNAUTHENTICATED, "Please sign in."))
                        .accessDeniedHandler((request, response, denied) ->
                                new ProblemAuthenticationEntryPoint(ErrorCode.FORBIDDEN, "Not allowed.")
                                        .commence(request, response, null)))
                .addFilterBefore(new SessionAuthenticationFilter(role, sessionCookies, jwtService, accounts),
                        UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Sign-in is handled by AuthService, not Spring's user-details flow. Declaring this bean
     * stops Spring Boot from creating a default user with a generated password.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Sign-in is handled by AuthService");
        };
    }
}
