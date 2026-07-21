package com.postSale.amcProject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig defines which endpoints need authentication and which are public.
 * It also configures session management and the password hashing algorithm.
 */
@Configuration
public class SecurityConfig {

    /**
     * PasswordEncoder bean — used to hash passwords before storing and to
     * verify passwords during login. BCrypt is the industry standard.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SecurityFilterChain defines the security rules for every HTTP request.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RestAuthenticationEntryPoint authEntryPoint) throws Exception {
        http
                // Disable CSRF - not needed for REST APIs using session cookies
                // (CSRF is mainly a concern for browser form submissions, not JSON APIs)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS using the rules defined in WebConfig.addCorsMappings()
                .cors(Customizer.withDefaults())

                // Disable HTTP Basic auth (we're doing our own login endpoint)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Disable the default login form
                .formLogin(AbstractHttpConfigurer::disable)

                // Use server-side sessions (Spring creates a session when needed)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // Return JSON 401 instead of HTML redirect when authentication fails
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authEntryPoint))

                // Define which endpoints are public and which require login
                .authorizeHttpRequests(auth -> auth
                        // Allow preflight OPTIONS requests (needed for CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth endpoints are public (no login needed to login/signup)
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()

                        // These business endpoints require authentication
                        .requestMatchers(
                                "/products/**",
                                "/customers/**",
                                "/sales/**",
                                "/warranty/**",
                                "/amcs/**",
                                "/amc-offers/**"
                        ).authenticated()

                        // Everything else (static files, /api/auth/me, /api/auth/logout) is permitted
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}

