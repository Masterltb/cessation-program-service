package com.smokefree.program.config;

import com.smokefree.program.auth.DevAutoUserFilter;
import com.smokefree.program.auth.HeaderUserContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // ⬅️ THÊM bean này (nếu class cần Environment, giữ ctor(Environment env))
    @Bean
    HeaderUserContextFilter headerUserContextFilter(Environment env) {
        return new HeaderUserContextFilter(env);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http,
                                 Environment env,
                                 DevAutoUserFilter dev,
                                 HeaderUserContextFilter header) throws Exception {
        http.csrf(csrf -> csrf.disable());

        // Bật JWT ở môi trường KHÁC dev
        if (!env.acceptsProfiles(Profiles.of("dev"))) {
            http.oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/error").permitAll()
                .anyRequest().authenticated()
        );

        // Dev filter đứng TRƯỚC Bearer
        http.addFilterBefore(dev, BearerTokenAuthenticationFilter.class);
        // Header filter chạy SAU dev filter
        http.addFilterAfter(header, DevAutoUserFilter.class);

        return http.build();
    }
}
