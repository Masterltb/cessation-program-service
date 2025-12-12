package com.smokefree.program.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bộ chuyển đổi JWT tùy chỉnh cho AWS Cognito JWT Tokens.
 * <p>
 * Trích xuất thông tin người dùng từ các claim của Cognito JWT:
 * - sub: ID người dùng (UUID)
 * - cognito:groups: Nhóm người dùng (admin, coach, customer)
 * <p>
 * Ánh xạ các nhóm này thành các quyền (authorities) của Spring Security với tiền tố "ROLE_".
 */
@Component
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /**
     * Chuyển đổi đối tượng JWT thành AbstractAuthenticationToken.
     *
     * @param jwt Token JWT đầu vào.
     * @return Token xác thực chứa thông tin principal và quyền hạn.
     */
    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // Sử dụng claim 'sub' làm principal (User ID)
        String principal = jwt.getClaimAsString("sub");

        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    /**
     * Trích xuất danh sách quyền hạn từ claim 'cognito:groups'.
     *
     * @param jwt Token JWT.
     * @return Danh sách các GrantedAuthority.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Trích xuất claim cognito:groups
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        if (groups != null && !groups.isEmpty()) {
            // Ánh xạ mỗi nhóm thành ROLE_<TÊN_NHÓM_IN_HOA>
            for (String group : groups) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()));
            }
        }

        return authorities;
    }
}
