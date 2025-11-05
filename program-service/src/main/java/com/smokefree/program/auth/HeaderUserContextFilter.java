package com.smokefree.program.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

public class HeaderUserContextFilter extends OncePerRequestFilter {

    private final Environment env;

    public HeaderUserContextFilter(Environment env) {
        this.env = env;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Log nhận thông tin từ header
        String uid = req.getHeader("X-User-Id");
        String role = req.getHeader("X-User-Role");

        // Log toàn bộ các thông tin header để kiểm tra
        System.out.println("Received headers - X-User-Id: " + uid + ", X-User-Role: " + role);

        // Kiểm tra nếu đã có auth (do filter khác set) thì bỏ qua
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("Authentication already set, skipping HeaderUserContextFilter.");
            chain.doFilter(req, res);
            return;
        }

        // Kiểm tra môi trường, xem có phải dev hay không
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");
        System.out.println("Is dev profile: " + isDev);

        // Kiểm tra nếu thiếu header, return lỗi nếu không phải môi trường dev
        if (uid == null || role == null || uid.isBlank() || role.isBlank()) {
            if (isDev) {
                System.out.println("Dev environment: Passing request to next filter.");
                chain.doFilter(req, res);
                return;
            }

            // Log lỗi thiếu thông tin
            System.out.println("Unauthorized: Missing or invalid X-User-Id / X-User-Role");

            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");
            res.getWriter().write("""
                {"error":"unauthorized","message":"Missing or invalid X-User-Id / X-User-Role"}
                """);
            return;
        }

        // Log việc tiếp tục với request nếu header hợp lệ
        System.out.println("Valid headers received. Passing request to next filter.");
        chain.doFilter(req, res);
    }
}
