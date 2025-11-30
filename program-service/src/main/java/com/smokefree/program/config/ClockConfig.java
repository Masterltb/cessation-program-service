package com.smokefree.program.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

/**
 * Cung cấp một Clock bean cho toàn bộ ứng dụng để có thể thay thế (mock) trong khi kiểm thử.
 * Điều này giúp loại bỏ việc gọi phương thức tĩnh java.time.LocalDate.now() trực tiếp trong code.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        // Cung cấp đồng hồ hệ thống mặc định theo múi giờ UTC.
        return Clock.systemUTC();
    }
}
