package com.smokefree.program.util;

import com.smokefree.program.auth.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authenticated user: " + (auth != null ? auth.getPrincipal() : "Anonymous"));

        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof UserPrincipal up) {
            return up.getUserId();
        }
        return null;
    }
}

