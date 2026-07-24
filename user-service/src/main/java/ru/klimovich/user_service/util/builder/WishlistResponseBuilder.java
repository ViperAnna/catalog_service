package ru.klimovich.user_service.util.builder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.responce.ProductResponse;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.mapper.WishlistMapper;
import ru.klimovich.user_service.model.Wishlist;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WishlistResponseBuilder {

    private final WishlistMapper wishlistMapper;
    private final CatalogClient catalogClient;

    public WishlistResponse buildWishlistResponse(Wishlist wishlist, String accessToken) {

        WishlistResponse response = wishlistMapper.toDTO(wishlist);

        if (wishlist.getProductIds().isEmpty()) {
            response.setProducts(Collections.emptySet());
            return response;
        }

        List<ProductResponse> products = catalogClient.getProductsByIds(new HashSet<>(wishlist.getProductIds()), accessToken);

        Map<String, ProductResponse> productMap = products
                .stream()
                .collect(Collectors.toMap(
                        ProductResponse::getId,
                        Function.identity()
                ));

        Set<ProductResponse> wishlistProducts = wishlist.getProductIds()
                .stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        response.setProducts(wishlistProducts);

        return response;
    }
}
