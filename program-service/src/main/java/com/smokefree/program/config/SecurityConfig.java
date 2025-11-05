package com.smokefree.program.config;

import com.smokefree.program.auth.HeaderUserContextFilter;
import com.smokefree.program.auth.DevAutoUserFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.Optional;

@Configuration
public class SecurityConfig {

    private final Optional<DevAutoUserFilter> devAuto;

    // Khởi tạo SecurityConfig với DevAutoUserFilter (nếu có)
    @Autowired
    public SecurityConfig(@Autowired(required = false) DevAutoUserFilter devAuto) {
        this.devAuto = Optional.ofNullable(devAuto);
    }

    // Bean cho HeaderUserContextFilter
    @Bean
    public HeaderUserContextFilter headerUserContextFilter(Environment env) {
        return new HeaderUserContextFilter(env);
    }

    // Cấu hình HttpSecurity để xác thực và bảo mật các endpoint
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Environment env) throws Exception {
        // In ra log để theo dõi quá trình cấu hình
        System.out.println("Configuring HttpSecurity for authentication.");

        // Cấu hình CSRF và các quy tắc xác thực
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/error").permitAll()  // Cho phép truy cập không cần xác thực đối với /actuator/** và /error
                .anyRequest().authenticated());  // Yêu cầu xác thực đối với tất cả các request còn lại

        // Thêm HeaderUserContextFilter vào chuỗi filter
        HeaderUserContextFilter headerFilter = headerUserContextFilter(env);
        System.out.println("Adding HeaderUserContextFilter to the filter chain.");
        http.addFilterBefore(headerFilter, BasicAuthenticationFilter.class);

        // Nếu profile là 'dev', thêm DevAutoUserFilter vào chuỗi filter
        devAuto.ifPresent(f -> {
            System.out.println("Adding DevAutoUserFilter to the filter chain.");
            http.addFilterAfter(f, HeaderUserContextFilter.class);
        });

        // Trả về cấu hình filter
        return http.build();
    }
}
