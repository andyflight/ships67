package com.games.shipmaster.common.api;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless bearer-token API. Only the session endpoints are public; everything else needs an access JWT
 * (validated by the JwtDecoder bean from auth infrastructure).
 */
@Configuration
public class SecurityConfig {

    static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/guest",
            "/api/v1/auth/guest/restore",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
    };

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, SecurityProblemHandler problems) throws Exception {
        http
                // No cookies or browser sessions: tokens travel in headers, so CSRF does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
                        // Local dev console; disabled in tests and to be disabled in production config.
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .oauth2ResourceServer(rs -> rs
                        .jwt(withDefaults())
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems));
        return http.build();
    }
}
