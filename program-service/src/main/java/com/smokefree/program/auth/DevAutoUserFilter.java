package com.smokefree.program.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DevAutoUserFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Log kiểm tra xem đã có Authentication chưa
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("No authentication found in SecurityContext. Proceeding with auto-assignment.");

            // Lấy thông tin từ headers hoặc gán giá trị mặc định
            String uid = Optional.ofNullable(request.getHeader("X-User-Id"))
                    .orElse("00000000-0000-0000-0000-000000000001");
            String role = Optional.ofNullable(request.getHeader("X-User-Role"))
                    .orElse("CUSTOMER");

            // Log thông tin UID và Role
            System.out.println("Auto-assigning UID: " + uid + " and Role: " + role);

            // Gán quyền cho người dùng
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            // Tạo Authentication và gán vào SecurityContext
            Authentication authentication = new UsernamePasswordAuthenticationToken(uid, "dummy", authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Log thông tin đã được gán Authentication
            System.out.println("Authentication set for user: " + uid + " with role: " + role);
        } else {
            // Nếu đã có authentication
            System.out.println("Authentication already set in SecurityContext.");
        }

        // Tiếp tục với chain
        chain.doFilter(request, response);
    }
}
