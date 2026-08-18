package ru.seller_service.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;


public interface CurrentUserService {

    Jwt getJwt();

    String getUserId();

    String getEmail();

    List<String> getRoles();

    String getAccessToken();
}


