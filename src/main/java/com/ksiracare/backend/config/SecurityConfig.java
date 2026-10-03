package com.ksiracare.backend.config;

import com.ksiracare.backend.exception.ErrorCode;
import com.ksiracare.backend.repository.TherapistRepository;
import com.ksiracare.backend.security.JwtService;
import com.ksiracare.backend.security.ProblemAuthenticationEntryPoint;
import com.ksiracare.backend.security.SessionAuthenticationFilter;
import com.ksiracare.backend.security.SessionCookies;
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
 * Stateless, cookie-based security. Only sign-in and sign-out are public; everything else
 * needs a valid session cookie. CSRF tokens aren't used: the cookie is SameSite=Strict and
 * the API only accepts JSON, so other sites can't make authenticated requests.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SessionCookies sessionCookies,
                                                   JwtService jwtService,
                                                   TherapistRepository therapistRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Only reachable when springdoc is enabled (development).
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new ProblemAuthenticationEntryPoint(
                                ErrorCode.UNAUTHENTICATED, "Please sign in."))
                        .accessDeniedHandler((request, response, denied) ->
                                new ProblemAuthenticationEntryPoint(ErrorCode.FORBIDDEN, "Not allowed.")
                                        .commence(request, response, null)))
                .addFilterBefore(new SessionAuthenticationFilter(sessionCookies, jwtService, therapistRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
