package ru.klimovich.user_service.util.builder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.responce.ItemResponse;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.mapper.WishlistItemMapper;
import ru.klimovich.user_service.mapper.WishlistMapper;
import ru.klimovich.user_service.model.Wishlist;
import ru.klimovich.user_service.model.WishlistItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WishlistResponseBuilder {

    private final WishlistMapper wishlistMapper;
    private final CatalogClient catalogClient;
    private final WishlistItemMapper wishlistItemMapper;

    public WishlistResponse buildWishlistResponse(Wishlist wishlist, String accessToken) {

        WishlistResponse response = wishlistMapper.toDTO(wishlist);

        List<String> productIds =
                wishlist.getItems()
                        .stream()
                        .map(WishlistItem::getProductId)
                        .toList();

        if (productIds.isEmpty()) {
            response.setItems(Collections.emptyList());
            return response;
        }

        List<ItemResponse> products = catalogClient.getProductsByIds(productIds, accessToken);

        Map<String, ItemResponse> productMap = products
                .stream()
                .collect(Collectors.toMap(
                        ItemResponse::getId,
                        Function.identity()
                ));

        List<ItemResponse> items = wishlist.getItems()
                .stream()
                .map(item ->
                    productMap.get(item.getProductId())
                    )
                .toList();

        response.setItems(items);

        return response;
    }
}
