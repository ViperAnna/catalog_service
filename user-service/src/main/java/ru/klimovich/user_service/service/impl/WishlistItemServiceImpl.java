package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.request.WishlistItemRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.exception.ResourceConflictException;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.mapper.WishlistItemMapper;
import ru.klimovich.user_service.model.Wishlist;
import ru.klimovich.user_service.model.WishlistItem;
import ru.klimovich.user_service.repository.WishlistItemRepository;
import ru.klimovich.user_service.repository.WishlistRepository;
import ru.klimovich.user_service.service.WishlistItemService;
import ru.klimovich.user_service.util.MessageKeys;
import ru.klimovich.user_service.util.builder.WishlistResponseBuilder;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistItemServiceImpl implements WishlistItemService {

    private final WishlistRepository wishlistRepo;
    public final WishlistItemMapper wishlistItemMapper;
    private final WishlistItemRepository itemRepo;
    private final CurrentUserService currentUserService;
    private final CatalogClient catalogClient;
    private final WishlistResponseBuilder responseBuilder;

    @Override
    public WishlistResponse addItem(Long wishlistId, WishlistItemRequest request) {
        String accessToken = currentUserService.getAccessToken();

        Wishlist wishlist = getWishlist(wishlistId);

        validateProduct(request.getProductId(), accessToken);

        checkDuplicate(wishlistId, request.getProductId());

        WishlistItem item = wishlistItemMapper.toEntity(request);
        item.setWishlist(wishlist);

        wishlist.getItems().add(item);

        itemRepo.save(item);

        return responseBuilder.buildWishlistResponse(wishlist, accessToken);
    }

    private Wishlist getWishlist(Long wishlistId) {
        return wishlistRepo.findByIdAndKeycloakUserId(wishlistId, currentUserService.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                String.format(MessageKeys.WISHLIST_NOT_FOUND, wishlistId)));
    }

    private void validateProduct(String productId, String accessToken) {
        catalogClient.getProductById(productId, accessToken);
    }

    private void checkDuplicate(Long wishlistId, String productId) {
        if (itemRepo.existsByWishlistIdAndProductId(wishlistId, productId)) {

            throw new ResourceConflictException(
                    MessageKeys.PRODUCT_ALREADY_IN_WISHLIST, productId);
        }
    }

    @Override
    public WishlistResponse removeItem(Long wishlistId, String productId) {

        Wishlist wishlist = getWishlist(wishlistId);

        WishlistItem wishlistItem = itemRepo

                .findByWishlistIdAndProductId(wishlistId, productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageKeys.WISHLIST_ITEM_NOT_FOUND, productId)
                );

        wishlist.getItems().remove(wishlistItem);

        return responseBuilder.buildWishlistResponse(wishlist, currentUserService.getAccessToken());
    }
}
