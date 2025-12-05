package com.smokefree.program.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DEV-ONLY: gán user/roles qua headers để test mà không cần JWT.
 *
 * Ưu tiên lấy thông tin theo thứ tự:
 * 1) X-Claims       : JSON { "principal": "...", "roles": ["ADMIN","COACH"] | [{ "authority": "ROLE_ADMIN" }, ...] }
 * 2) X-User-Groups  : "ADMIN,COACH"; dùng kèm X-User-Id
 * 3) X-User-Group   : một group; dùng kèm X-User-Id
 * 4) X-User-Tier    : ADMIN | COACH | VIP | COACH_VIP | BASIC  → map sang roles
 * 5) Mặc định       : principal = 00000000-0000-0000-0000-000000000001, roles = ROLE_CUSTOMER
 *
 * Có Bearer token → filter bỏ qua, trừ khi X-Dev-Override: 1|true.
 */
public class DevAutoUserFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(DevAutoUserFilter.class);
    private static final ObjectMapper M = new ObjectMapper();

    private static final String DEFAULT_PRINCIPAL = "00000000-0000-0000-0000-000000000001";

    // Header keys
    private static final String H_CLAIMS       = "X-Claims";
    private static final String H_USER_ID      = "X-User-Id";
    private static final String H_USER_GROUP   = "X-User-Group";
    private static final String H_USER_GROUPS  = "X-User-Groups";
    private static final String H_USER_TIER    = "X-User-Tier";
    private static final String H_DEV_OVERRIDE = "X-Dev-Override";
    private static final String H_AUTHZ        = "Authorization";

    // Tier → roles
    private static final Map<String, List<String>> TIER_ROLES = Map.of(
            "ADMIN",      List.of("ROLE_ADMIN"),
            "COACH",      List.of("ROLE_COACH"),
            "VIP",        List.of("ROLE_VIP"),
            "COACH_VIP",  List.of("ROLE_COACH", "ROLE_VIP"),
            "BASIC",      List.of("ROLE_CUSTOMER")
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // Có Bearer và không bật override → nhường JWT
        final String authz = req.getHeader(H_AUTHZ);
        final boolean bearerPresent = StringUtils.hasText(authz) && authz.trim().toLowerCase().startsWith("bearer ");
        final boolean overrideOn = isTruthy(req.getHeader(H_DEV_OVERRIDE));
        if (bearerPresent && !overrideOn) {
            chain.doFilter(req, res);
            return;
        }

        // Đã có auth hợp lệ và không bật override → đi tiếp
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated() && !overrideOn) {
            chain.doFilter(req, res);
            return;
        }

        // --- Lấy principal + roles ---
        String principal = null;
        List<String> roleNames = new ArrayList<>();

        // 1) X-Claims JSON
        String claimsJson = req.getHeader(H_CLAIMS);
        if (StringUtils.hasText(claimsJson)) {
            try {
                Map<String, Object> claims = M.readValue(claimsJson, new TypeReference<>() {});
                Object p = claims.get("principal");
                if (p != null) principal = String.valueOf(p);

                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof Collection<?> coll) {
                    for (Object it : coll) {
                        if (it instanceof String s) {
                            roleNames.add(s);
                        } else if (it instanceof Map<?, ?> m && m.get("authority") != null) {
                            roleNames.add(String.valueOf(m.get("authority")));
                        }
                    }
                }
                log.debug("[DevAuth] Parsed X-Claims principal={} roles={}", principal, roleNames);
            } catch (Exception e) {
                log.warn("[DevAuth] Không parse được X-Claims: {}", e.getMessage());
            }
        }

        // 2) X-User-Groups + X-User-Id
        if (roleNames.isEmpty()) {
            String groupsCsv = req.getHeader(H_USER_GROUPS);
            if (StringUtils.hasText(groupsCsv)) {
                roleNames = Stream.of(groupsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));
                principal = orDefault(principal, req.getHeader(H_USER_ID));
            }
        }

        // 3) X-User-Group + X-User-Id
        if (roleNames.isEmpty()) {
            String g = req.getHeader(H_USER_GROUP);
            if (StringUtils.hasText(g)) {
                roleNames.add(g.trim());
                principal = orDefault(principal, req.getHeader(H_USER_ID));
            }
        }

        // 4) X-User-Tier
        if (roleNames.isEmpty()) {
            String tier = req.getHeader(H_USER_TIER);
            if (StringUtils.hasText(tier)) {
                List<String> mapped = TIER_ROLES.get(tier.trim().toUpperCase());
                if (mapped != null) roleNames.addAll(mapped);
                principal = orDefault(principal, req.getHeader(H_USER_ID));
                log.debug("[DevAuth] Tier={} → roles={}", tier, mapped);
            }
        }

        // 5) Mặc định
        if (!StringUtils.hasText(principal)) principal = DEFAULT_PRINCIPAL;
        if (roleNames.isEmpty()) roleNames = List.of("ROLE_CUSTOMER");

        // Chuẩn hoá ROLE_ prefix
        List<GrantedAuthority> authorities = roleNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .distinct()
                .collect(Collectors.toList());

        // Tạo Authentication + gán vào SecurityContext theo chuẩn
        var auth = new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        log.info("[DevAuth] principal={} authorities={}", principal,
                authorities.stream().map(GrantedAuthority::getAuthority).toList());

        chain.doFilter(req, res);
    }

    private static boolean isTruthy(String v) {
        if (!StringUtils.hasText(v)) return false;
        String s = v.trim().toLowerCase();
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("y");
    }

    private static String orDefault(String base, String fallback) {
        return StringUtils.hasText(base) ? base : fallback;
    }
}
