package ru.seller_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrentUserServiceImpl implements CurrentUserService{

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

    @Override
    public String getEmail() {
        return getJwt().getClaimAsString("email");
    }

    @Override
    public List<String> getRoles() {
        return getJwt().getClaimAsStringList("roles");
    }

    public String getAccessToken() {
        return getJwt().getTokenValue();
    }
}

