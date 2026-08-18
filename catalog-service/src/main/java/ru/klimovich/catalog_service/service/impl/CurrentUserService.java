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

    public String getAccessToken() {
        return getJwt().getTokenValue();
    }
}
