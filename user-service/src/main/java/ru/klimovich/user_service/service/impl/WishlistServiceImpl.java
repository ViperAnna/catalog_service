package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.dto.responce.WishlistShortResponse;
import ru.klimovich.user_service.exception.ResourceConflictException;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.mapper.WishlistMapper;
import ru.klimovich.user_service.model.Wishlist;
import ru.klimovich.user_service.repository.WishlistRepository;
import ru.klimovich.user_service.service.WishlistService;
import ru.klimovich.user_service.util.MessageKeys;
import ru.klimovich.user_service.util.builder.WishlistResponseBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepo;
    private final WishlistMapper wishlistMapper;
    private final CurrentUserService currentUserService;
    private final WishlistResponseBuilder responseBuilder;
    private final CatalogClient catalogClient;

    @Override
    public WishlistResponse createWishlist(WishlistRequest wishlistDetails) {
        String userId = currentUserService.getUserId();
        validateUniqueName(userId, wishlistDetails.getName(), null);

        Wishlist wishlist = wishlistMapper.toEntity(wishlistDetails);
        wishlist.setKeycloakUserId(userId);

        return wishlistMapper.toDTO(wishlistRepo.save(wishlist));
    }

    @Override
    public List<WishlistShortResponse> getAllWishlistsByUser() {
        String keycloakUserId = currentUserService.getUserId();

        return wishlistRepo.findByKeycloakUserId(keycloakUserId)
                .stream()
                .map(wishlist -> {
                    WishlistShortResponse response = wishlistMapper.toShortDTO(wishlist);
                    response.setProductCount(wishlist.getProductIds().size());
                    return response;
                })

                .toList();
    }

    @Override
    public WishlistResponse getWishListById(Long wishlistId) {
        Wishlist wishlist = getWishlist(wishlistId);
        return responseBuilder.buildWishlistResponse(wishlist, currentUserService.getAccessToken());
    }

    @Override
    public WishlistResponse addProduct(Long wishlistId, String productId) {
        String accessToken = currentUserService.getAccessToken();

        Wishlist wishlist = getWishlist(wishlistId);
        catalogClient.getProductById(productId, accessToken);

        if (!wishlist.getProductIds().add(productId)) {
            throw new ResourceConflictException(MessageKeys.PRODUCT_ALREADY_IN_WISHLIST, productId);
        }

        wishlistRepo.save(wishlist);
        return responseBuilder.buildWishlistResponse(wishlist, accessToken);
    }

    private Wishlist getWishlist(Long wishlistId) {
        return wishlistRepo
                .findByIdAndKeycloakUserId(wishlistId, currentUserService.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                String.format(MessageKeys.WISHLIST_NOT_FOUND, wishlistId)
                        ));
    }


    @Override
    public WishlistResponse removeProduct(Long wishlistId, String productId) {
        String accessToken = currentUserService.getAccessToken();
        Wishlist wishlist = getWishlist(wishlistId);

        if (!wishlist.getProductIds().remove(productId)) {
            throw new ResourceConflictException(MessageKeys.PRODUCT_NOT_FOUND, productId);
        }
        wishlistRepo.save(wishlist);
        return responseBuilder.buildWishlistResponse(wishlist, accessToken);
    }

    @Override
    public WishlistResponse updateWishlist(Long wishlistId, WishlistRequest wishlistDetails) {
        String userId = currentUserService.getUserId();
        String accessToken = currentUserService.getAccessToken();

        validateUniqueName(userId, wishlistDetails.getName(), wishlistId);

        Wishlist wishlist = getWishlist(wishlistId);

        wishlistMapper.updateFromDTO(wishlistDetails, wishlist);
        wishlistRepo.save(wishlist);

        return responseBuilder.buildWishlistResponse(wishlist, accessToken);
    }

    private void validateUniqueName(String userId, String wishlistName, Long wishlistId) {
        boolean exists = wishlistId == null
                ? wishlistRepo.existsByKeycloakUserIdAndName(userId, wishlistName)
                : wishlistRepo.existsByKeycloakUserIdAndNameAndIdNot(
                userId,
                wishlistName,
                wishlistId
        );
        if (exists) {
            throw new ResourceConflictException(
                    String.format(
                            MessageKeys.WISHLIST_NAME_ALREADY_EXIST,
                            wishlistName
                    )
            );
        }
    }

    @Override
    public void deleteWishlist(Long wishlistId) {
        wishlistRepo.delete(getWishlist(wishlistId));
    }
}
