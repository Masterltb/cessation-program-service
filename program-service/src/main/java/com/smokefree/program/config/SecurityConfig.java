package com.smokefree.program.config;

import com.smokefree.program.auth.HeaderUserContextFilter;
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
public class SecurityConfig {

    @Bean
    public HeaderUserContextFilter headerUserContextFilter(Environment env) {
        return new HeaderUserContextFilter(env);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Environment env) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/error").permitAll()
                .anyRequest().authenticated()
        );

        // --- Bật JWT resource-server CHỈ KHI có config ---
        String issuer = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String jwkSet = env.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");

        if (issuer != null || jwkSet != null) {
            // chạy sau này, khi nối với gateway / keycloak / cognito...
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            // môi trường dev/local: không dùng JWT, cho phép auth từ filter tự custom
            http.httpBasic(Customizer.withDefaults()); // hoặc .anonymous(Customizer.withDefaults())
        }

        // luôn add filter đọc header X-User-Id, X-Roles,...
        http.addFilterBefore(headerUserContextFilter(env), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
