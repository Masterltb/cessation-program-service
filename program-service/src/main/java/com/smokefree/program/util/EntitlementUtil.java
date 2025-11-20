// src/main/java/com/smokefree/program/util/EntitlementUtil.java
package com.smokefree.program.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public final class EntitlementUtil {

    private EntitlementUtil() {}

    /**
     * Lấy entitlement tier hiện tại từ Authentication.details
     * (HeaderUserContextFilter nên set ent_tier / X-Ent-Tier ở đó).
     * Mặc định trả về "basic".
     */
    @SuppressWarnings("unchecked")
    public static String currentTier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "basic";

        Object details = auth.getDetails();
        if (details instanceof Map<?, ?> m) {
            Map<String, Object> map = (Map<String, Object>) m;
            Object raw = map.getOrDefault("ent_tier", map.get("X-Ent-Tier"));
            if (raw instanceof String s && !s.isBlank()) {
                return s.toLowerCase();
            }
        }
        return "basic";
    }
}
