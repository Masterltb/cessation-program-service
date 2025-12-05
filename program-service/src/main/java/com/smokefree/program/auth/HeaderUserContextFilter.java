package com.smokefree.program.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority; // Import GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList; // Sử dụng ArrayList
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Security Filter that extracts User Identity from HTTP Headers.
 * <p>
 * Expected Headers (from Upstream Gateway):
 * - X-User-Id: UUID of the authenticated user.
 * - X-User-Group: User group (ADMIN, COACH, CUSTOMER). Mapped to "ROLE_{GROUP}".
 * - X-User-Tier: (Optional) Subscription tier (BASIC, PREMIUM, VIP). Mapped to "TIER_{TIER}".
 * </p>
 * This filter constructs the {@link org.springframework.security.core.Authentication} object
 * tailored for the stateless microservice architecture.
 */
public class HeaderUserContextFilter extends OncePerRequestFilter {

    private final Environment env;

    public HeaderUserContextFilter(Environment env) {
        this.env = env;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {

        // Nếu đã có Authentication thì bỏ qua
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        // Đọc các header
        String uid = req.getHeader("X-User-Id");
        String group = req.getHeader("X-User-Group");
        String tier = req.getHeader("X-User-Tier"); // <-- Đọc thêm header tier

        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");

        // Thiếu header vai trò chính
        if (uid == null || uid.isBlank() || group == null || group.isBlank()) {
            if (isDev) { // dev thì cho qua
                chain.doFilter(req, res);
                return;
            }
            // Trả lỗi nếu không phải dev
            sendUnauthorizedResponse(res, "Missing or invalid X-User-Id / X-User-Group");
            return;
        }

        // Có header → validate & set Authentication
        try {
            UUID.fromString(uid.trim()); // Chỉ kiểm tra định dạng UUID

            // *** LOGIC NÂNG CẤP BẮT ĐẦU TỪ ĐÂY ***
            List<GrantedAuthority> authorities = new ArrayList<>();

            // 1. Thêm vai trò chính (ROLE_) - Map Group -> Role Authority
            authorities.add(new SimpleGrantedAuthority("ROLE_" + group.trim().toUpperCase()));

            // 2. Nếu là CUSTOMER và có tier, thêm quyền tier (TIER_)
            if ("CUSTOMER".equalsIgnoreCase(group.trim()) && tier != null && !tier.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("TIER_" + tier.trim().toUpperCase()));
            }
            // *** KẾT THÚC LOGIC NÂNG CẤP ***

            var auth = new UsernamePasswordAuthenticationToken(
                    uid.trim(), // Giữ principal là chuỗi UUID
                    "N/A",
                    authorities // <-- Sử dụng danh sách quyền đã được nâng cấp
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (IllegalArgumentException ex) {
            if (isDev) {
                chain.doFilter(req, res);
                return;
            }
            sendUnauthorizedResponse(res, "X-User-Id must be a valid UUID");
            return;
        }

        chain.doFilter(req, res);
    }

    private void sendUnauthorizedResponse(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write(
                "{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}"
        );
    }
}