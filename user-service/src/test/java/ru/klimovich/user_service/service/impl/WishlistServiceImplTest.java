package ru.klimovich.user_service.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.exception.ResourceConflictException;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.mapper.WishlistMapper;
import ru.klimovich.user_service.model.Wishlist;
import ru.klimovich.user_service.repository.WishlistRepository;
import ru.klimovich.user_service.util.builder.WishlistResponseBuilder;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Requirement "Removing product from wishlist" и "Wishlist rename" (change wishlist-error-responses).
 */
@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    private static final String USER_ID = "kc-user";

    @Mock
    private WishlistRepository wishlistRepo;
    @Mock
    private WishlistMapper wishlistMapper;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private WishlistResponseBuilder responseBuilder;
    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @BeforeEach
    void setUp() {
        when(currentUserService.getUserId()).thenReturn(USER_ID);
    }

    private static Wishlist wishlist(Long id, String name, Set<String> productIds) {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(id);
        wishlist.setName(name);
        wishlist.setKeycloakUserId(USER_ID);
        wishlist.setProductIds(new HashSet<>(productIds));
        return wishlist;
    }

    private static WishlistRequest request(String name) {
        WishlistRequest request = new WishlistRequest();
        request.setName(name);
        return request;
    }

    @Test
    @DisplayName("Удаление товара, которого нет в вишлисте: 404 с WISHLIST_ITEM_NOT_FOUND, без сохранения")
    void removeMissingProduct() {
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID))
                .thenReturn(Optional.of(wishlist(1L, "Gifts", Set.of("p-2"))));

        assertThatThrownBy(() -> wishlistService.removeProduct(1L, "p-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id [p-1] not found in wishlist.");
        verify(wishlistRepo, never()).save(any());
    }

    @Test
    @DisplayName("Переименование несуществующего вишлиста при занятом имени: 404, имя не проверяется")
    void renameMissingWishlistWithTakenName() {
        when(wishlistRepo.findByIdAndKeycloakUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.updateWishlist(99L, request("Taken")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("The wishlist with id [99] not found.");
        verify(wishlistRepo, never()).existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(anyString(), anyString(), anyLong());
        verify(wishlistRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Переименование своего вишлиста в имя другого своего вишлиста: 409, без сохранения")
    void renameToTakenName() {
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID))
                .thenReturn(Optional.of(wishlist(1L, "Gifts", Set.of())));
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(USER_ID, "Books", 1L)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.updateWishlist(1L, request("Books")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("The wishlist with name [Books] already exist");
        verify(wishlistRepo, never()).saveAndFlush(any());
    }

    // ---------- change user-service-db-constraints: имя без регистра и пробелов по краям ----------

    @Test
    @DisplayName("Создание '  Books  ': сохраняется 'Books', проверка уникальности по обрезанному имени")
    void createTrimsName() {
        Wishlist mapped = wishlist(null, "  Books  ", Set.of());
        when(wishlistMapper.toEntity(any(WishlistRequest.class))).thenReturn(mapped);
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCase(USER_ID, "Books")).thenReturn(false);
        when(wishlistRepo.saveAndFlush(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));

        wishlistService.createWishlist(request("  Books  "));

        ArgumentCaptor<Wishlist> saved = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Books");
        assertThat(saved.getValue().getKeycloakUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Создание 'gifts' при существующем 'Gifts': 409 с обрезанным именем")
    void createDuplicateIgnoringCase() {
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCase(USER_ID, "gifts")).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.createWishlist(request(" gifts ")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("The wishlist with name [gifts] already exist");
        verify(wishlistRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Переименование своего 'Gifts' в 'GIFTS': проходит, свой вишлист исключен из проверки")
    void renameChangingCaseOfOwnName() {
        Wishlist own = wishlist(1L, "Gifts", Set.of());
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID)).thenReturn(Optional.of(own));
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(USER_ID, "GIFTS", 1L)).thenReturn(false);
        when(wishlistRepo.saveAndFlush(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));

        wishlistService.updateWishlist(1L, request("GIFTS"));

        assertThat(own.getName()).isEqualTo("GIFTS");
        verify(wishlistRepo).saveAndFlush(own);
    }

    @Test
    @DisplayName("Переименование в ' books ' при существующем 'Books': 409, имя не меняется")
    void renameToTakenNameIgnoringCaseAndSpaces() {
        Wishlist own = wishlist(2L, "Gifts", Set.of());
        when(wishlistRepo.findByIdAndKeycloakUserId(2L, USER_ID)).thenReturn(Optional.of(own));
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(USER_ID, "books", 2L)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.updateWishlist(2L, request(" books ")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("The wishlist with name [books] already exist");
        assertThat(own.getName()).isEqualTo("Gifts");
        verify(wishlistRepo, never()).saveAndFlush(any());
    }

    // ---------- change user-service-db-constraints: гонка с уникальным индексом -> 409 ----------

    @Test
    @DisplayName("Одновременное создание: нарушение uq_wishlists_user_name дает 409")
    void createRaceOnUniqueIndex() {
        when(wishlistMapper.toEntity(any(WishlistRequest.class))).thenReturn(wishlist(null, "Gifts", Set.of()));
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCase(USER_ID, "Gifts")).thenReturn(false);
        when(wishlistRepo.saveAndFlush(any(Wishlist.class)))
                .thenThrow(violation("uq_wishlists_user_name", "23505"));

        assertThatThrownBy(() -> wishlistService.createWishlist(request("Gifts")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("The wishlist with name [Gifts] already exist");
    }

    @Test
    @DisplayName("Одновременное переименование: нарушение uq_wishlists_user_name дает 409")
    void renameRaceOnUniqueIndex() {
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID))
                .thenReturn(Optional.of(wishlist(1L, "Old", Set.of())));
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(USER_ID, "Gifts", 1L)).thenReturn(false);
        when(wishlistRepo.saveAndFlush(any(Wishlist.class)))
                .thenThrow(violation("uq_wishlists_user_name", "23505"));

        assertThatThrownBy(() -> wishlistService.updateWishlist(1L, request("Gifts")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("The wishlist with name [Gifts] already exist");
    }

    @Test
    @DisplayName("Одновременное добавление товара: нарушение uq_wishlist_products дает 409")
    void addProductRaceOnUniqueIndex() {
        Wishlist own = wishlist(1L, "Gifts", Set.of());
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID)).thenReturn(Optional.of(own));
        when(wishlistRepo.findByIdAndKeycloakUserIdForUpdate(1L, USER_ID)).thenReturn(Optional.of(own));
        when(wishlistRepo.saveAndFlush(any(Wishlist.class)))
                .thenThrow(violation("uq_wishlist_products", "23505"));

        assertThatThrownBy(() -> wishlistService.addProduct(1L, "p-1"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Product with id [p-1] already exists in wishlist.");
    }

    @Test
    @DisplayName("Другое нарушение целостности не переводится в 409")
    void otherIntegrityViolationPropagates() {
        when(wishlistMapper.toEntity(any(WishlistRequest.class))).thenReturn(wishlist(null, "Gifts", Set.of()));
        when(wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCase(USER_ID, "Gifts")).thenReturn(false);
        DataIntegrityViolationException other = violation("some_other_constraint", "23505");
        when(wishlistRepo.saveAndFlush(any(Wishlist.class))).thenThrow(other);

        assertThatThrownBy(() -> wishlistService.createWishlist(request("Gifts"))).isSameAs(other);
    }

    // ---------- change wishlist-access-and-limits: удаление без зависимости от каталога ----------

    @Test
    @DisplayName("Удаление товара: id удален и сохранен до сборки ответа, ответ не зависит от каталога")
    void removeProductDoesNotDependOnCatalog() {
        Wishlist own = wishlist(1L, "Gifts", Set.of("p-1", "p-2"));
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID)).thenReturn(Optional.of(own));

        wishlistService.removeProduct(1L, "p-1");

        assertThat(own.getProductIds()).containsExactly("p-2");
        InOrder order = inOrder(wishlistRepo, responseBuilder);
        order.verify(wishlistRepo).saveAndFlush(own);
        order.verify(responseBuilder).buildWishlistResponseOrWithoutProducts(eq(own), any());
        verify(responseBuilder, never()).buildWishlistResponse(any(), any());
    }

    // ---------- change wishlist-access-and-limits: лимиты 20 / 200 ----------

    @Test
    @DisplayName("Лимит вишлистов: при 20 вишлистах 409; блокировка пользователя до подсчета")
    void createAtWishlistLimit() {
        when(wishlistRepo.countByKeycloakUserId(USER_ID)).thenReturn(20L);

        assertThatThrownBy(() -> wishlistService.createWishlist(request("New")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Wishlist limit reached: at most 20 wishlists per user.");

        InOrder order = inOrder(wishlistRepo);
        order.verify(wishlistRepo).lockUserWishlists("wishlists:" + USER_ID);
        order.verify(wishlistRepo).countByKeycloakUserId(USER_ID);
        verify(wishlistRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Ниже лимита вишлистов: создание проходит")
    void createBelowWishlistLimit() {
        when(wishlistRepo.countByKeycloakUserId(USER_ID)).thenReturn(19L);
        when(wishlistMapper.toEntity(any(WishlistRequest.class))).thenReturn(wishlist(null, "New", Set.of()));
        when(wishlistRepo.saveAndFlush(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));

        wishlistService.createWishlist(request("New"));

        verify(wishlistRepo).saveAndFlush(any(Wishlist.class));
    }

    @Test
    @DisplayName("Лимит товаров: при 200 товарах 409; каталог до блокировки, проверка под блокировкой")
    void addProductAtProductLimit() {
        Set<String> full = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            full.add("p-" + i);
        }
        Wishlist own = wishlist(1L, "Gifts", full);
        when(wishlistRepo.findByIdAndKeycloakUserId(1L, USER_ID)).thenReturn(Optional.of(own));
        when(wishlistRepo.findByIdAndKeycloakUserIdForUpdate(1L, USER_ID)).thenReturn(Optional.of(own));

        assertThatThrownBy(() -> wishlistService.addProduct(1L, "p-new"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Wishlist product limit reached: at most 200 products per wishlist.");

        InOrder order = inOrder(catalogClient, wishlistRepo);
        order.verify(catalogClient).getProductById(eq("p-new"), any());
        order.verify(wishlistRepo).findByIdAndKeycloakUserIdForUpdate(1L, USER_ID);
        verify(wishlistRepo, never()).saveAndFlush(any());
    }

    private static DataIntegrityViolationException violation(String constraint, String sqlState) {
        SQLException sql = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"" + constraint + "\"", sqlState);
        return new DataIntegrityViolationException("could not execute statement", sql);
    }
}
