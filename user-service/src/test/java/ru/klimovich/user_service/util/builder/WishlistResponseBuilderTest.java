package ru.klimovich.user_service.util.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.responce.ProductResponse;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.exception.CatalogServiceException;
import ru.klimovich.user_service.mapper.WishlistMapper;
import ru.klimovich.user_service.model.Wishlist;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Change wishlist-access-and-limits (D4): ответ после удаления товара не зависит от каталога.
 */
@ExtendWith(MockitoExtension.class)
class WishlistResponseBuilderTest {

    @Mock
    private WishlistMapper wishlistMapper;
    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private WishlistResponseBuilder builder;

    @Test
    @DisplayName("Id физически удаленного товара остался в вишлисте: товар не показывается, id не удаляется")
    void deletedProductIsNotShownAndIdKept() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setProductIds(new HashSet<>(Set.of("p-alive", "p-deleted")));
        ProductResponse alive = new ProductResponse("p-alive", "Alive", null, null, List.of());
        when(wishlistMapper.toDTO(any(Wishlist.class))).thenReturn(new WishlistResponse(1L, "Gifts", null, null, null));
        when(catalogClient.getProductsByIds(anySet(), eq("token"))).thenReturn(List.of(alive));

        WishlistResponse response = builder.buildWishlistResponse(wishlist, "token");

        assertThat(response.getProducts()).extracting(ProductResponse::getId).containsExactly("p-alive");
        assertThat(wishlist.getProductIds()).containsExactlyInAnyOrder("p-alive", "p-deleted");
    }

    @Test
    @DisplayName("Каталог недоступен: вишлист без данных товаров, сохраненные id не меняются")
    void catalogUnavailableGivesEmptyProducts() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setProductIds(new HashSet<>(Set.of("p-2")));
        when(wishlistMapper.toDTO(any(Wishlist.class))).thenReturn(new WishlistResponse(1L, "Gifts", null, null, null));
        when(catalogClient.getProductsByIds(anySet(), eq("token")))
                .thenThrow(new CatalogServiceException("Catalog service is unavailable."));

        WishlistResponse response = builder.buildWishlistResponseOrWithoutProducts(wishlist, "token");

        assertThat(response.getProducts()).isEmpty();
        assertThat(wishlist.getProductIds()).containsExactly("p-2");
    }
}
