package ru.seller_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.seller_service.security.SpringSecurityConfig;
import ru.seller_service.service.SellerApplicationService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Change separate-seller-account (D4): регистрация — USER; просмотр своих заявок — USER или SELLER;
 * изменение, удаление и разблокировка — SELLER. Проверяется только фильтр безопасности:
 * разрешенный запрос не дает 401/403 (ответ определяет контроллер с моком сервиса).
 */
@WebMvcTest(controllers = SellerApplicationController.class)
@Import(SpringSecurityConfig.class)
@TestPropertySource(properties = {
        "spring.application.name=seller-service",
        "server.port=0",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost/unused"
})
class SellerApplicationAccessWebMvcTest {

    private static final String USER = "user-token";
    private static final String SELLER = "seller-token";
    private static final String USER_SELLER = "user-seller-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SellerApplicationService sellerApplicationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        stub(USER, "USER");
        stub(SELLER, "SELLER");
        stub(USER_SELLER, "USER", "SELLER");
    }

    private void stub(String token, String... roles) {
        when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("kc-" + token)
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
    }

    private static ResultMatcher allowed() {
        return result -> {
            int code = result.getResponse().getStatus();
            if (code == 401 || code == 403) {
                throw new AssertionError("Expected access, got " + code);
            }
        };
    }

    private void perform(MockHttpServletRequestBuilder request, String token, ResultMatcher expected) throws Exception {
        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(expected);
    }

    private static MockHttpServletRequestBuilder registration() {
        return post("/seller-applications/registration").contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private static MockHttpServletRequestBuilder update() {
        return put("/seller-applications/update").contentType(MediaType.MULTIPART_FORM_DATA);
    }

    private static MockHttpServletRequestBuilder deletion() {
        return delete("/seller-applications/deletion").contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    private static MockHttpServletRequestBuilder unblock() {
        return post("/seller-applications/unblock").contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    @Test
    @DisplayName("Регистрация: только USER")
    void registrationRequiresUser() throws Exception {
        perform(registration(), USER, allowed());
        perform(registration(), SELLER, status().isForbidden());
    }

    @Test
    @DisplayName("Свои заявки и история: USER или SELLER")
    void ownApplicationsForUserAndSeller() throws Exception {
        perform(get("/seller-applications/me"), USER, allowed());
        perform(get("/seller-applications/me"), SELLER, allowed());
        perform(get("/seller-applications/history"), USER, allowed());
        perform(get("/seller-applications/history"), SELLER, allowed());
    }

    @Test
    @DisplayName("Изменение, удаление, разблокировка: только SELLER")
    void sellerOperationsRequireSeller() throws Exception {
        perform(update(), USER, status().isForbidden());
        perform(deletion(), USER, status().isForbidden());
        perform(unblock(), USER, status().isForbidden());
        perform(update(), SELLER, allowed());
        perform(deletion(), SELLER, allowed());
        perform(unblock(), SELLER, allowed());
    }

    @Test
    @DisplayName("Существующий продавец с USER и SELLER: доступ ко всем операциям, как раньше")
    void legacySellerKeepsAccess() throws Exception {
        perform(registration(), USER_SELLER, allowed());
        perform(update(), USER_SELLER, allowed());
        perform(get("/seller-applications/me"), USER_SELLER, allowed());
    }

    @Test
    @DisplayName("Без токена: 401")
    void noToken() throws Exception {
        mockMvc.perform(get("/seller-applications/me")).andExpect(status().isUnauthorized());
    }
}
