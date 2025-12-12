package com.smokefree.program.auth;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

/**
 * Đối tượng Principal tùy chỉnh đại diện cho người dùng đã được xác thực trong hệ thống.
 * Implement interface Authentication của Spring Security để lưu trữ thông tin phiên làm việc.
 */
@Getter
@Builder
public class UserPrincipal implements Authentication {
    /** ID định danh duy nhất của người dùng. */
    private final UUID userId;

    /** Tập hợp các vai trò (roles) của người dùng (ví dụ: ROLE_CUSTOMER, ROLE_ADMIN). */
    private final Set<String> roles;

    /** Hạng thành viên của người dùng (basic, premium, vip). */
    private final String tier;        // basic|premium|vip

    /** Múi giờ của người dùng (ví dụ: Asia/Ho_Chi_Minh). */
    private final String timezone;    // ví dụ Asia/Ho_Chi_Minh

    /** Trạng thái quyền lợi (Entitlement State): TRIALING, ACTIVE, GRACE, EXPIRED. */
    private final String entState;    // TRIALING|ACTIVE|GRACE|EXPIRED

    /** Thời điểm hết hạn quyền lợi. */
    private final Instant entExpiresAt;

    /** Trạng thái xác thực (mặc định là true khi đối tượng này được tạo thành công). */
    private boolean authenticated = true;

    /**
     * Lấy danh sách các quyền hạn (Authorities) dựa trên roles.
     *
     * @return Collection các GrantedAuthority.
     */
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null) return List.of();
        return roles.stream().map(SimpleGrantedAuthority::new).toList();
    }

    /**
     * Lấy thông tin chứng thực (password/token).
     * Trả về null vì chúng ta không lưu password trong principal sau khi xác thực để bảo mật.
     */
    @Override public Object getCredentials() { return null; }

    /**
     * Lấy thông tin chi tiết bổ sung về request xác thực (thường là IP, Session ID...).
     */
    @Override public Object getDetails() { return null; }

    /**
     * Lấy đối tượng chính (Principal), ở đây trả về userId.
     */
    @Override public Object getPrincipal() { return userId; }

    /**
     * Kiểm tra xem người dùng đã được xác thực chưa.
     */
    @Override public boolean isAuthenticated() { return authenticated; }

    /**
     * Thiết lập trạng thái xác thực.
     */
    @Override public void setAuthenticated(boolean isAuthenticated) { this.authenticated = isAuthenticated; }

    /**
     * Lấy tên định danh của người dùng (trả về userId dạng chuỗi hoặc "anonymous").
     */
    @Override public String getName() { return userId != null ? userId.toString() : "anonymous"; }
}
