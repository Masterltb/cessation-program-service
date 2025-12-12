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

/**
 * Cấu hình bảo mật chính cho ứng dụng Spring Boot.
 * Thiết lập các quy tắc phân quyền, bộ lọc xác thực và tích hợp OAuth2 Resource Server (Cognito).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CognitoJwtAuthenticationConverter jwtAuthenticationConverter;

    /**
     * Bean tạo bộ lọc để đọc thông tin người dùng từ Header.
     * Thường được sử dụng trong môi trường Dev hoặc khi service chạy sau API Gateway đã xác thực.
     *
     * @param env Môi trường hiện tại để kiểm tra cấu hình.
     * @return HeaderUserContextFilter
     */
    @Bean
    public HeaderUserContextFilter headerUserContextFilter(Environment env) {
        return new HeaderUserContextFilter(env);
    }

    /**
     * Định nghĩa chuỗi bộ lọc bảo mật (Security Filter Chain).
     * Cấu hình quyền truy cập dựa trên URL và Role, cũng như cơ chế xác thực (JWT hoặc Basic).
     *
     * @param http Đối tượng HttpSecurity để cấu hình.
     * @param env  Môi trường hiện tại.
     * @return SecurityFilterChain đã được cấu hình.
     * @throws Exception Nếu có lỗi trong quá trình cấu hình.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   Environment env) throws Exception {

        // Tắt CSRF vì ứng dụng sử dụng API stateless (RESTful)
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // Các endpoint công khai (Public)
                .requestMatchers("/actuator/**", "/error").permitAll()

                // Các endpoint dành riêng cho Admin
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/debug/**").hasRole("ADMIN")

                // Các endpoint dành cho Coach hoặc Admin
                .requestMatchers("/coach/**").hasAnyRole("COACH", "ADMIN")

                // Các endpoint dành cho Khách hàng (Customer)
                .requestMatchers("/api/me/**").hasRole("CUSTOMER")
                .requestMatchers("/api/onboarding/**").hasRole("CUSTOMER")
                .requestMatchers("/v1/programs/**").hasRole("CUSTOMER")
                .requestMatchers("/v1/me/**").hasRole("CUSTOMER")
                .requestMatchers("/api/programs/**").hasRole("CUSTOMER")
                .requestMatchers("/api/plan-templates/**").hasRole("CUSTOMER")
                .requestMatchers("/api/modules/**").hasRole("CUSTOMER")
                .requestMatchers("/api/subscriptions/**").hasRole("CUSTOMER")

                // Tất cả các yêu cầu khác đều yêu cầu xác thực
                .anyRequest().authenticated()
        );

        // --- Bật JWT resource-server CHỈ KHI có config ---
        String issuer = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String jwkSet = env.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");

        if (issuer != null || jwkSet != null) {
            // Production: Sử dụng JWT từ AWS Cognito với bộ chuyển đổi tùy chỉnh
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
            ));
        } else {
            // Dev/Local: Sử dụng bộ lọc header tùy chỉnh hoặc Basic Auth để xác thực
            http.httpBasic(Customizer.withDefaults());
        }

        // Luôn thêm filter đọc header X-User-Id, X-Roles,... trước UsernamePasswordAuthenticationFilter
        // Điều này giúp đồng bộ context người dùng từ Gateway hoặc Mock trong môi trường Dev
        http.addFilterBefore(headerUserContextFilter(env), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
