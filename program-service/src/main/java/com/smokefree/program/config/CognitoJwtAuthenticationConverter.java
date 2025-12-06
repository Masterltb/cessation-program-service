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
 * Custom JWT Converter for AWS Cognito JWT Tokens.
 * <p>
 * Extracts user information from Cognito JWT claims:
 * - sub: User ID (UUID)
 * - cognito:groups: User groups (admin, coach, customer)
 * <p>
 * Maps groups to Spring Security authorities with "ROLE_" prefix.
 */
@Component
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // Use 'sub' claim as principal (User ID)
        String principal = jwt.getClaimAsString("sub");

        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extract cognito:groups claim
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");

        if (groups != null && !groups.isEmpty()) {
            // Map each group to ROLE_<GROUP_UPPERCASE>
            for (String group : groups) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + group.toUpperCase()));
            }
        }

        return authorities;
    }
}
