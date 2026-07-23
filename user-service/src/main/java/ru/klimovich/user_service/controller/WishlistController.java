package ru.klimovich.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.user_service.dto.Response;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
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
    public List<WishlistResponse> getAllWishlistsByUser() {
        return wishlistService.getAllWishlistsByUser();
    }

    @GetMapping("/{id}")
    public WishlistResponse getWishlistById(@PathVariable Long id) {
        return wishlistService.getWishListById(id);
    }

    @PutMapping("/{id}")
    public Response updateWishList(@PathVariable Long id, @Valid @RequestBody WishlistRequest wishlistDetails) {
        wishlistService.updateWishlist(id, wishlistDetails);
        return new Response(
                WISHLIST_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{id}")
    public Response deleteWishListById(@PathVariable Long id) {
        wishlistService.deleteWishlist(id);
        return new Response(
                WISHLIST_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}