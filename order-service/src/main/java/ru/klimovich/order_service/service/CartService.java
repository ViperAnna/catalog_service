package ru.klimovich.order_service.service;

import ru.klimovich.order_service.dto.request.AddCartItemRequest;
import ru.klimovich.order_service.dto.request.UpdateCartItemRequest;
import ru.klimovich.order_service.dto.response.CartItemResponse;
import ru.klimovich.order_service.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart();

    CartItemResponse addItem(AddCartItemRequest request);

    CartItemResponse updateQuantity(String productId, UpdateCartItemRequest request);

    void removeItem(String productId);

    void clear();
}
