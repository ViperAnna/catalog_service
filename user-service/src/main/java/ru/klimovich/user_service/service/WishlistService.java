package ru.klimovich.user_service.service;

import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;

import java.util.List;

public interface WishlistService {
    WishlistResponse createWishlist(WishlistRequest wishlistDetails);

    List<WishlistResponse> getAllWishlistsByUser();

    WishlistResponse getWishListById(Long id);

    WishlistResponse addProduct(Long wishlistId, String productId);

    WishlistResponse removeProduct(Long wishlistId, String productId);


    WishlistResponse updateWishlist(Long id, WishlistRequest wishlistDetails);

    void deleteWishlist(Long id);

}
