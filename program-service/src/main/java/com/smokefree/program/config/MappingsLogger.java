package com.smokefree.program.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// com.smokefree.program.config.MappingsLogger
/**
 * Cấu hình tiện ích để ghi log tất cả các đường dẫn API (endpoints) đã được đăng ký khi ứng dụng khởi động.
 * Giúp dễ dàng kiểm tra và debug các URL đang hoạt động.
 */
@Slf4j
@Configuration
public class MappingsLogger {

    /**
     * Bean CommandLineRunner này sẽ chạy ngay sau khi Spring Context khởi tạo xong.
     * Nó duyệt qua tất cả các handler methods trong RequestMappingHandlerMapping và ghi log.
     *
     * @param mapping Bean quản lý các request mapping của Spring MVC.
     * @return CommandLineRunner thực thi việc ghi log.
     */
    @Bean
    CommandLineRunner logMappings(
            @Qualifier("requestMappingHandlerMapping")
            RequestMappingHandlerMapping mapping) {
        return args -> {
            mapping.getHandlerMethods().forEach((info, handler) -> {
                // Lấy danh sách các đường dẫn (URL patterns)
                var paths = info.getPathPatternsCondition() != null
                        ? info.getPathPatternsCondition().getPatternValues()
                        : (info.getPatternsCondition() != null
                        ? info.getPatternsCondition().getPatterns()
                        : java.util.Set.of());

                // Lấy các phương thức HTTP (GET, POST, ...)
                var methods = info.getMethodsCondition().getMethods();

                // Ghi log thông tin: [API] [GET] [/v1/api/...] -> com.example.Controller.method(...)
                log.info("[API] {} {} -> {}", methods, paths, handler.getMethod().toGenericString());
            });
        };
    }
}
