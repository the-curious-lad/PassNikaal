package com.passnikaal.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Spring Security configuration for outpass-core.
 *
 * This is the SINGLE source of truth for all access control rules.
 * All endpoint permissions live HERE - not in @PreAuthorize annotations
 * scattered across controllers. Reading this one file tells you exactly
 * what is accessible to whom.
 *
 * STAGE 1 STATUS: permitAll() so the health endpoint works without auth.
 * Stage 4 will add the JwtAuthenticationFilter and full RBAC rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * SecurityFilterChain bean.
     *
     * Defines the HTTP security filter chain. Every incoming request
     * passes through this chain before reaching any controller.
     *
     * @param http  HttpSecurity builder provided by Spring
     * @return      The configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // CSRF disabled: this is a stateless JWT API, not a server-rendered
            // form app. CSRF attacks require cookie-based sessions, which we don't use.
            .csrf(AbstractHttpConfigurer::disable)

            // STATELESS sessions: Spring will not create or use HttpSessions.
            // Every request must carry its own JWT access token.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // TODO Stage 4: Replace permitAll() with the full RBAC matrix.
            // Example of what the final rules will look like:
            //   .requestMatchers(POST, "/api/v1/auth/**").permitAll()
            //   .requestMatchers(POST, "/api/v1/outpasses").hasRole("STUDENT")
            //   .requestMatchers(POST, "/api/v1/outpasses/*/approve").hasRole("APPROVER")
            //   .requestMatchers("/api/v1/gate/**").hasRole("GATE_GUARD")
            //   .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            //   .anyRequest().authenticated()
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * PasswordEncoder bean - BCrypt with strength 10.
     *
     * BCrypt automatically generates a unique salt per password, so two
     * identical passwords produce different hashes. Strength 10 = 2^10
     * iterations - a good balance of security and performance.
     *
     * Declared here (not in AuthService) to avoid circular dependency
     * issues with Spring Security's internal wiring.
     *
     * Used in Stage 3 by AuthService to hash passwords on registration
     * and verify them on login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}