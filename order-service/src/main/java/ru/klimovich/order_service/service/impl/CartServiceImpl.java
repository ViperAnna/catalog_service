package ru.klimovich.order_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.order_service.domain.AvailableProduct;
import ru.klimovich.order_service.domain.CartPricing;
import ru.klimovich.order_service.dto.request.AddCartItemRequest;
import ru.klimovich.order_service.dto.request.UpdateCartItemRequest;
import ru.klimovich.order_service.dto.response.CartItemResponse;
import ru.klimovich.order_service.dto.response.CartLineResponse;
import ru.klimovich.order_service.dto.response.CartResponse;
import ru.klimovich.order_service.exception.ResourceConflictException;
import ru.klimovich.order_service.exception.ResourceNotFoundException;
import ru.klimovich.order_service.model.Cart;
import ru.klimovich.order_service.model.CartItem;
import ru.klimovich.order_service.repository.CartItemRepository;
import ru.klimovich.order_service.repository.CartRepository;
import ru.klimovich.order_service.security.CurrentUserService;
import ru.klimovich.order_service.service.CartService;
import ru.klimovich.order_service.util.MessageKeys;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Корзина текущего пользователя (spec shopping-cart). Корзина адресуется только по sub,
 * все изменения выполняются под блокировкой строки корзины.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    static final int MAX_CART_ITEMS = 50;

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final CartLocker cartLocker;
    private final CatalogAvailability catalogAvailability;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Optional<Cart> cart = cartRepo.findByUserId(currentUserService.getUserId());
        List<CartItem> items = cart.map(cartItemRepo::findByCartOrderByCreatedAtAscIdAsc).orElse(List.of());
        if (items.isEmpty()) {
            return new CartResponse(List.of(), CartPricing.total(List.of()), 0);
        }

        Map<String, AvailableProduct> available = catalogAvailability.availableProducts(
                items.stream().map(CartItem::getProductId).toList(),
                currentUserService.getAccessToken());

        List<CartLineResponse> lines = new ArrayList<>();
        List<BigDecimal> lineTotals = new ArrayList<>();
        for (CartItem item : items) {
            AvailableProduct product = available.get(item.getProductId());
            if (product == null) {
                lines.add(CartLineResponse.unavailable(item.getProductId(), item.getQuantity()));
                continue;
            }
            BigDecimal lineTotal = CartPricing.lineTotal(product.unitPrice(), item.getQuantity());
            lineTotals.add(lineTotal);
            lines.add(new CartLineResponse(item.getProductId(), item.getQuantity(), true,
                    product.name(), product.storeId(), product.unitPrice(), lineTotal));
        }
        return new CartResponse(lines, CartPricing.total(lineTotals), items.size());
    }

    @Override
    @Transactional
    public CartItemResponse addItem(AddCartItemRequest request) {
        String productId = request.productId();
        Cart cart = cartLocker.lockOrCreate(currentUserService.getUserId());

        if (cartItemRepo.findByCartAndProductId(cart, productId).isPresent()) {
            throw new ResourceConflictException(MessageKeys.CART_ITEM_ALREADY_EXISTS, productId, productId);
        }
        if (cartItemRepo.countByCart(cart) >= MAX_CART_ITEMS) {
            throw new ResourceConflictException(MessageKeys.CART_ITEMS_LIMIT_EXCEEDED, MAX_CART_ITEMS);
        }
        Map<String, AvailableProduct> available = catalogAvailability.availableProducts(
                List.of(productId), currentUserService.getAccessToken());
        if (!available.containsKey(productId)) {
            throw new ResourceNotFoundException(MessageKeys.PRODUCT_NOT_AVAILABLE, productId);
        }

        CartItem item = cartItemRepo.save(CartItem.builder()
                .cart(cart)
                .productId(productId)
                .quantity(request.quantity())
                .build());
        return new CartItemResponse(item.getProductId(), item.getQuantity());
    }

    @Override
    @Transactional
    public CartItemResponse updateQuantity(String productId, UpdateCartItemRequest request) {
        CartItem item = lockedItem(productId);
        item.setQuantity(request.quantity());
        return new CartItemResponse(item.getProductId(), item.getQuantity());
    }

    @Override
    @Transactional
    public void removeItem(String productId) {
        cartItemRepo.delete(lockedItem(productId));
    }

    @Override
    @Transactional
    public void clear() {
        cartLocker.lock(currentUserService.getUserId())
                .ifPresent(cartItemRepo::deleteAllByCart);
    }

    private CartItem lockedItem(String productId) {
        Cart cart = cartLocker.lock(currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.CART_ITEM_NOT_FOUND, productId));
        return cartItemRepo.findByCartAndProductId(cart, productId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.CART_ITEM_NOT_FOUND, productId));
    }
}
