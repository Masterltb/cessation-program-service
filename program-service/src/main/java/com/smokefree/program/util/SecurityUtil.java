package com.smokefree.program.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tiện ích hỗ trợ bảo mật (Security Utility).
 * Cung cấp các phương thức tĩnh để truy xuất thông tin người dùng hiện tại (ID, Roles, VIP status)
 * từ SecurityContext của Spring Security.
 */
public final class SecurityUtil {
    private SecurityUtil() {}

    // --- USER ID ---
    /**
     * Lấy ID của người dùng hiện tại từ SecurityContext.
     * Hỗ trợ lấy từ Principal là UUID, String hoặc đối tượng có phương thức getId().
     *
     * @return UUID của người dùng hoặc null nếu chưa xác thực hoặc không tìm thấy.
     */
    public static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        Object p = auth.getPrincipal();
        if (p == null) return null;
        if (p instanceof UUID u) return u;
        try {
            // Thử dùng reflection để gọi getId() nếu principal là một đối tượng tùy chỉnh (ví dụ UserPrincipal)
            var m = p.getClass().getMethod("getId");
            Object id = m.invoke(p);
            if (id instanceof UUID u) return u;
            if (id instanceof String s) return UUID.fromString(s);
        } catch (Exception ignore) {}
        if (p instanceof String s) {
            try { return UUID.fromString(s); } catch (IllegalArgumentException ignore) {}
        }
        return null;
    }

    /**
     * Lấy ID người dùng hiện tại, ném ngoại lệ nếu không tìm thấy (chưa đăng nhập).
     *
     * @return UUID của người dùng.
     * @throws IllegalStateException Nếu không có người dùng nào được xác thực.
     */
    public static UUID requireUserId() {
        UUID id = currentUserId();
        if (id == null) throw new IllegalStateException("Missing authenticated user");
        return id;
    }

    // --- ROLES ---
    /**
     * Lấy tập hợp các vai trò (Roles) của người dùng hiện tại.
     * Tự động loại bỏ tiền tố "ROLE_" nếu có.
     *
     * @return Set các tên vai trò (String).
     */
    public static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(a -> a == null ? null : a.getAuthority())
                .filter(Objects::nonNull)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .collect(Collectors.toSet());
    }

    /**
     * Kiểm tra người dùng hiện tại có vai trò cụ thể hay không.
     *
     * @param role Tên vai trò cần kiểm tra (không bao gồm tiền tố ROLE_).
     * @return true nếu có, ngược lại false.
     */
    public static boolean hasRole(String role) {
        return currentRoles().contains(role);
    }

    /**
     * Kiểm tra người dùng hiện tại có bất kỳ vai trò nào trong danh sách hay không.
     *
     * @param roles Danh sách các vai trò cần kiểm tra.
     * @return true nếu có ít nhất một vai trò khớp.
     */
    public static boolean hasAnyRole(String... roles) {
        Set<String> cur = currentRoles();
        for (String r : roles) if (cur.contains(r)) return true;
        return false;
    }

    // --- VIP ---
    /**
     * Kiểm tra xem người dùng có quyền VIP đối với một chương trình cụ thể hay không.
     * Thông tin VIP thường được lưu trong phần 'details' của Authentication.
     *
     * @param programId ID của chương trình.
     * @return true nếu là VIP của chương trình đó.
     */
    public static boolean isVip(UUID programId) {
        if (programId == null) return false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Set<UUID> vip = extractVipPrograms(auth.getDetails());
        return vip.contains(programId);
    }

    @SuppressWarnings("unchecked")
    private static Set<UUID> extractVipPrograms(Object details) {
        if (details instanceof Map<?, ?> m) {
            Map<String, Object> map = (Map<String, Object>) m;
            // Tìm key vip_programs hoặc header X-Vip-Programs
            Object val = map.getOrDefault("vip_programs", map.get("X-Vip-Programs"));
            return toUuidSet(val);
        }

        // details là chuỗi CSV hoặc một collection
        if (details instanceof String s) return parseCsvUUIDs(s);
        if (details instanceof Collection<?> c) return toUuidSet(c);
        return Set.of();
    }

    private static Set<UUID> toUuidSet(Object val) {
        if (val == null) return Set.of();
        if (val instanceof UUID u) return Set.of(u);
        if (val instanceof String s) return parseCsvUUIDs(s);
        if (val instanceof Collection<?> c) {
            return c.stream().map(SecurityUtil::coerceUUID)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    private static Set<UUID> parseCsvUUIDs(String s) {
        if (s == null || s.isBlank()) return Set.of();
        return Arrays.stream(s.split("[,;\\s]+"))
                .map(SecurityUtil::coerceUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static UUID coerceUUID(Object o) {
        if (o instanceof UUID u) return u;
        if (o instanceof String s) {
            try { return UUID.fromString(s.trim()); } catch (Exception ignore) {}
        }
        return null;
    }
}
