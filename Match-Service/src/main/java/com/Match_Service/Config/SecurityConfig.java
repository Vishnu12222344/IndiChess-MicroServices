package com.Match_Service.Config;

import com.Match_Service.Security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Match Service.
 *
 * CORS is disabled - API Gateway handles it for REST endpoints.
 * WebSocketConfig handles CORS for WebSocket connections.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS DISABLED - Gateway handles it
                .cors(AbstractHttpConfigurer::disable)

                // ✅ Disable CSRF (we use JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ Stateless session
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Handle authentication failures
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // ✅ Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow WebSocket connections (auth handled by WebSocketAuthInterceptor)
                        .requestMatchers("/ws/**").permitAll()
                        // Allow actuator endpoints (for monitoring)
                        .requestMatchers("/actuator/**").permitAll()
                        // All match endpoints require authentication
                        .anyRequest().authenticated()
                )

                // ✅ JWT filter for REST endpoints
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}