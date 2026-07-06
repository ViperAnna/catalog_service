package ru.klimovich.user_service.service;

import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;

import java.util.List;

public interface WishlistService {
    WishlistResponse createWishlist(WishlistRequest wishlistDetails, String keycloakUserId);

    List<WishlistResponse> getAllWishlistsByUser(String keycloakUserId);

    List<WishlistResponse> getWishlistsByUser(String keycloakUserId);

    WishlistResponse getWishListById(Long id, String keycloakUserId);

    List<WishlistResponse> getWishlistByName(String name, String keycloakUserId);


    WishlistResponse updateWishlist(Long id, WishlistRequest wishlistDetails, String keycloakUserId);

    void deleteWishlist(Long id, String keycloakUserId);

}
