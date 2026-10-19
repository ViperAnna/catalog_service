package ru.klimovich.catalog_service.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Jwt getJwt() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return (Jwt) authentication.getPrincipal();
    }

    public String getUserId() {
        return getJwt().getSubject();
    }

    /** true, если запрос с JWT и ролью SELLER; анонимный запрос — false. */
    public boolean isSeller() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof Jwt
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SELLER".equals(a.getAuthority()));
    }

    public String getAccessToken() {
        return getJwt().getTokenValue();
    }
}
