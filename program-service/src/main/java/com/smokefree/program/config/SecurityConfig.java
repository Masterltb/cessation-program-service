package com.smokefree.program.config;

import com.smokefree.program.auth.HeaderUserContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CognitoJwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public HeaderUserContextFilter headerUserContextFilter(Environment env) {
        return new HeaderUserContextFilter(env);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Environment env) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/**", "/error").permitAll()

                // Admin-only endpoints
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/debug/**").hasRole("ADMIN")

                // Coach or Admin endpoints
                .requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")

                // Customer-only endpoints
                .requestMatchers("/api/me/**").hasRole("CUSTOMER")
                .requestMatchers("/api/onboarding/**").hasRole("CUSTOMER")
                .requestMatchers("/v1/programs/**").hasRole("CUSTOMER")
                .requestMatchers("/v1/me/**").hasRole("CUSTOMER")
                .requestMatchers("/api/programs/**").hasRole("CUSTOMER")
                .requestMatchers("/api/plan-templates/**").hasRole("CUSTOMER")
                .requestMatchers("/api/modules/**").hasRole("CUSTOMER")
                .requestMatchers("/api/subscriptions/**").hasRole("CUSTOMER")

                // All other endpoints require authentication
                .anyRequest().authenticated()
        );

        // --- Bật JWT resource-server CHỈ KHI có config ---
        String issuer = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String jwkSet = env.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");

        if (issuer != null || jwkSet != null) {
            // Production: Use JWT from AWS Cognito with custom converter
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
            ));
        } else {
            // Dev/Local: Use custom header filter for authentication
            http.httpBasic(Customizer.withDefaults());
        }

        // luôn add filter đọc header X-User-Id, X-Roles,...
        http.addFilterBefore(headerUserContextFilter(env), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
