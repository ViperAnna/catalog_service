package ru.klimovich.order_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Текущий пользователь из JWT. Идентичность — только claim sub (keycloakUserId).
 */
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

    /**
     * Email для уведомлений: claim email, только если email_verified = true; иначе null.
     */
    public String getVerifiedEmail() {
        Jwt jwt = getJwt();
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            return null;
        }
        return email;
    }
}
