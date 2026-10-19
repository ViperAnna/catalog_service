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
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.exception.CatalogServiceException;
import ru.klimovich.user_service.exception.ProductNotFoundException;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.security.SpringSecurityConfig;
import ru.klimovich.user_service.service.WishlistService;
import ru.klimovich.user_service.util.MessageKeys;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requirement "Wishlist error responses", "Adding product to wishlist" и "Removing product from wishlist"
 * (change wishlist-error-responses): ошибки каталога и вишлиста возвращаются с телом {message, time}.
 */
@WebMvcTest(controllers = WishlistController.class)
@Import(SpringSecurityConfig.class)
@TestPropertySource(properties = {
        "spring.application.name=user-service",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
class WishlistWebMvcTest {

    private static final String USER_TOKEN = "user-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final String USER_SELLER_TOKEN = "user-seller-token";
    private static final String ADMIN_SELLER_TOKEN = "admin-seller-token";
    private static final String ADMIN_TOKEN = "admin-token";

    @BeforeEach
    void setUp() {
        stubToken(USER_TOKEN, "USER");
        stubToken(USER_SELLER_TOKEN, "USER", "SELLER");
        stubToken(ADMIN_SELLER_TOKEN, "ADMIN", "SELLER");
        stubToken(ADMIN_TOKEN, "ADMIN");
    }

    private void stubToken(String token, String... roles) {
        when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("kc-" + token)
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
    }

    // ---------- change wishlist-access-and-limits: роль SELLER не получает доступ ----------

    @Test
    @DisplayName("USER+SELLER: 403 на GET и POST /wishlists")
    void userWithSellerRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/wishlists").header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_SELLER_TOKEN))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/wishlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_SELLER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gifts\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(wishlistService);
    }

    @Test
    @DisplayName("ADMIN+SELLER: 403")
    void adminWithSellerRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/wishlists/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_SELLER_TOKEN))
                .andExpect(status().isForbidden());
        verifyNoInteractions(wishlistService);
    }

    @Test
    @DisplayName("Только USER и только ADMIN: доступ сохраняется")
    void userAndAdminWithoutSellerAllowed() throws Exception {
        when(wishlistService.getAllWishlistsByUser()).thenReturn(List.of());
        mockMvc.perform(get("/wishlists").header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN))
                .andExpect(status().isOk());
        mockMvc.perform(get("/wishlists").header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание вишлиста: 201 с телом {message, time, id}")
    void createReturnsId() throws Exception {
        when(wishlistService.createWishlist(any(WishlistRequest.class)))
                .thenReturn(new WishlistResponse(42L, "Gifts", Set.of(), null, null));

        mockMvc.perform(post("/wishlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gifts\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("The wishlist was successfully created"))
                .andExpect(jsonPath("$.time", notNullValue()))
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    @DisplayName("Товар не найден в каталоге: 404 с телом {message, time}")
    void productNotFoundInCatalog() throws Exception {
        when(wishlistService.addProduct(eq(1L), eq("p-1")))
                .thenThrow(new ProductNotFoundException(MessageKeys.PRODUCT_NOT_FOUND, "p-1"));

        mockMvc.perform(post("/wishlists/1/products/p-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id [p-1] not found."))
                .andExpect(jsonPath("$.time", notNullValue()));
    }

    @Test
    @DisplayName("Каталог недоступен: 503 с телом {message, time}")
    void catalogUnavailable() throws Exception {
        when(wishlistService.addProduct(eq(1L), eq("p-1")))
                .thenThrow(new CatalogServiceException(MessageKeys.CATALOG_SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/wishlists/1/products/p-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Catalog service is unavailable."))
                .andExpect(jsonPath("$.time", notNullValue()));
    }

    @Test
    @DisplayName("Каталог недоступен при чтении вишлиста: 503 с телом {message, time}")
    void catalogUnavailableOnRead() throws Exception {
        when(wishlistService.getWishListById(1L))
                .thenThrow(new CatalogServiceException(MessageKeys.CATALOG_SERVICE_UNAVAILABLE));

        mockMvc.perform(get("/wishlists/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Catalog service is unavailable."));
    }

    @Test
    @DisplayName("Товара нет в вишлисте: 404 с телом {message, time}")
    void itemNotInWishlist() throws Exception {
        when(wishlistService.removeProduct(eq(1L), eq("p-1")))
                .thenThrow(new ResourceNotFoundException(MessageKeys.WISHLIST_ITEM_NOT_FOUND, "p-1"));

        mockMvc.perform(delete("/wishlists/1/products/p-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + USER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id [p-1] not found in wishlist."));
    }
}
