package com.smokefree.program.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        // Logging thông tin khi bị từ chối truy cập
        System.out.println("Access denied: " + authException.getMessage());

        // Trả về mã lỗi 403 (Forbidden)
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
    }
}
