package ru.klimovich.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.user_service.dto.Response;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.dto.responce.WishlistShortResponse;
import ru.klimovich.user_service.service.WishlistService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.klimovich.user_service.util.MessageKeys.*;

@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response createWishList(@Valid @RequestBody WishlistRequest wishlistDetails) {
        wishlistService.createWishlist(wishlistDetails);
        return new Response(
                WISHLIST_CREATED_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @GetMapping()
    public List<WishlistShortResponse> getAllWishlistsByUser() {
        return wishlistService.getAllWishlistsByUser();
    }


    @GetMapping("/{wishlistId}")
    public WishlistResponse getWishlistById(@PathVariable Long wishlistId) {
        return wishlistService.getWishListById(wishlistId);
    }

    @PutMapping("/{wishlistId}")
    public Response updateWishlist(@PathVariable Long wishlistId, @Valid @RequestBody WishlistRequest wishlistDetails) {
        wishlistService.updateWishlist(wishlistId, wishlistDetails);
        return new Response(
                WISHLIST_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{wishlistId}")
    public Response deleteWishlistById(@PathVariable Long wishlistId) {
        wishlistService.deleteWishlist(wishlistId);
        return new Response(
                WISHLIST_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }


    @PostMapping("/{wishlistId}/products/{productId}")
    public WishlistResponse addProduct(@PathVariable Long wishlistId, @PathVariable String productId) {
        return wishlistService.addProduct(wishlistId, productId);
    }

    @DeleteMapping("/{wishlistId}/products/{productId}")
    public WishlistResponse removeProduct(@PathVariable Long wishlistId, @PathVariable String productId) {
        return wishlistService.removeProduct(wishlistId, productId);
    }
}