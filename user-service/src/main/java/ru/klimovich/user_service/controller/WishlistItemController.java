package ru.klimovich.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.user_service.dto.request.WishlistItemRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.service.impl.WishlistItemServiceImpl;

@RestController
@RequestMapping("/wishlists/{wishlistId}/items")
@RequiredArgsConstructor
public class WishlistItemController {

    private final WishlistItemServiceImpl itemService;

    @PostMapping()
    public WishlistResponse addItem(@PathVariable Long wishlistId, @RequestBody @Valid WishlistItemRequest request) {
        return itemService.addItem(wishlistId, request);
    }

    @DeleteMapping("/{productId}")
    public WishlistResponse removeItem(@PathVariable Long wishlistId, @PathVariable String productId) {
        return itemService.removeItem(wishlistId, productId);
    }
}