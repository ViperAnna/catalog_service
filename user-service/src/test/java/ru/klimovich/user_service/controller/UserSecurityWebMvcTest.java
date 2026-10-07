package ru.klimovich.user_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.klimovich.user_service.dto.responce.UserResponse;
import ru.klimovich.user_service.security.SpringSecurityConfig;
import ru.klimovich.user_service.service.UserService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Фиксирует baseline-поведение requirement "User profile API access",
 * "Profile field validation" и "User service error responses"
 * (openspec/specs/user-profile/spec.md).
 *
 * <p>JWT проходит через реальную цепочку SpringSecurityConfig ->
 * JwtAuthenticationConverter -> KCRoleConverter: подменяется только JwtDecoder,
 * запрос отправляется с настоящим заголовком Authorization.
 */
@WebMvcTest(controllers = {UserController.class, AdminController.class})
@Import(SpringSecurityConfig.class)
@TestPropertySource(properties = {
        "spring.application.name=user-service",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
class UserSecurityWebMvcTest {

    private static final String USER_TOKEN = "user-token";
    private static final String LOWERCASE_USER_TOKEN = "lowercase-user-token";
    private static final String ADMIN_TOKEN = "admin-token";
    private static final String NO_ROLES_TOKEN = "no-roles-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static Jwt jwt(String token, Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("kc-" + token)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        claims.forEach(builder::claim);
        return builder.build();
    }

    private static Map<String, Object> realmRoles(String... roles) {
        return Map.of("realm_access", Map.of("roles", List.of(roles)));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(USER_TOKEN)).thenReturn(jwt(USER_TOKEN, realmRoles("USER")));
        when(jwtDecoder.decode(LOWERCASE_USER_TOKEN)).thenReturn(jwt(LOWERCASE_USER_TOKEN, realmRoles("user")));
        when(jwtDecoder.decode(ADMIN_TOKEN)).thenReturn(jwt(ADMIN_TOKEN, realmRoles("ADMIN")));
        when(jwtDecoder.decode(NO_ROLES_TOKEN)).thenReturn(jwt(NO_ROLES_TOKEN, Map.of("email", "x@example.com")));
        when(userService.getUser(any(Jwt.class))).thenReturn(new UserResponse());
        when(userService.getAllUsers()).thenReturn(List.of());
    }

    @Test
    @DisplayName("User profile API access: /users/me без токена -> 401")
    void usersMe_withoutToken_401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User profile API access: /users/me с ролью USER -> 200")
    void usersMe_withUserRole_200() throws Exception {
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User profile API access: роль user в нижнем регистре сопоставляется с ROLE_USER")
    void usersMe_withLowercaseUserRole_200() throws Exception {
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(LOWERCASE_USER_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("User profile API access: /users/me только с ролью ADMIN -> 403")
    void usersMe_withAdminOnly_403() throws Exception {
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User profile API access: JWT без realm_access -> 403")
    void usersMe_withoutRealmAccess_403() throws Exception {
        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(NO_ROLES_TOKEN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User profile API access: /admin/users без токена -> 401")
    void adminUsers_withoutToken_401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User profile API access: /admin/users с ролью USER -> 403")
    void adminUsers_withUserRole_403() throws Exception {
        mockMvc.perform(get("/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("User profile API access: /admin/users с ролью ADMIN -> 200")
    void adminUsers_withAdminRole_200() throws Exception {
        mockMvc.perform(get("/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(ADMIN_TOKEN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Own profile deletion: DELETE /users/me с ролью USER -> 204")
    void deleteUsersMe_withUserRole_204() throws Exception {
        mockMvc.perform(delete("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Profile field validation / User service error responses: невалидный phone -> 400 {message, time}")
    void updateUsersMe_invalidPhone_400WithValidationMessage() throws Exception {
        mockMvc.perform(put("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(USER_TOKEN))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("phone", "12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Validation failed:")))
                .andExpect(jsonPath("$.time").exists());
    }
}
