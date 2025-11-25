package com.smokefree.program.domain.service.auth;

import com.smokefree.program.util.SecurityUtil;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("authz")
public class AuthorizationHelper {
    public boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN");
    }
    public boolean isCoach() {
        return SecurityUtil.hasAnyRole("ADMIN", "COACH");
    }

    public boolean isAdmin(@Nullable UUID programId) {
        return SecurityUtil.hasRole("ADMIN");
    }
    public boolean isCoach(@Nullable UUID programId) {
        return SecurityUtil.hasAnyRole("ADMIN", "COACH");
    }
    public boolean isVip(@Nullable UUID programId) {
        return SecurityUtil.isVip(programId);
    }
}

