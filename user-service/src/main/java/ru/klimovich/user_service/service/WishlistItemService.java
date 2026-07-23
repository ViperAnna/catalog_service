package ru.klimovich.user_service.service;

import ru.klimovich.user_service.dto.request.WishlistItemRequest;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;

import java.util.List;

public interface WishlistItemService {

    WishlistResponse addItem(Long wishlistId, WishlistItemRequest request);

    WishlistResponse removeItem(Long wishlistId, String productId);


}
