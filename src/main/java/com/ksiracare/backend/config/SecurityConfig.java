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
 * Stateless, cookie-based security. Everyone signs in at {@code /auth/login}; the role in the
 * session token decides what it opens:
 * <ul>
 *   <li>{@code /public/**}, {@code GET /languages} — the website's booking form, no sign-in;</li>
 *   <li>{@code /admin/**} — the admin console, admins only;</li>
 *   <li>{@code /auth/session}, {@code /auth/password} — any signed-in account;</li>
 *   <li>everything else — the aapta portal, aaptas only.</li>
 * </ul>
 * Signed in but the wrong role is 403; not signed in is 401. CSRF tokens aren't used: the cookie
 * is SameSite=Strict and the API only accepts JSON, so other sites can't make authenticated requests.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionCookies sessionCookies;
    private final JwtService jwtService;
    private final AccountDirectory accounts;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                        // The website's booking form; clients have no accounts.
                        .requestMatchers("/public/**").permitAll()
                        // The language list feeds the website's booking form too; it is not private.
                        .requestMatchers(HttpMethod.GET, "/languages").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Only reachable when springdoc is enabled (development).
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/auth/**").authenticated()
                        .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                        .anyRequest().hasRole(Role.AAPTA.name()))
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
                .addFilterBefore(new SessionAuthenticationFilter(sessionCookies, jwtService, accounts),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
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
