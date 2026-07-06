package ru.klimovich.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public Response createWishList(@Valid @RequestBody WishlistRequest wishlistDetails, @AuthenticationPrincipal Jwt jwt) {

        wishlistService.createWishlist(wishlistDetails, jwt.getSubject());
        return new Response(
                WISHLIST_CREATED_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @GetMapping()
    public List<WishlistResponse> getAllWishlistsByUser(@AuthenticationPrincipal Jwt jwt) {

        return wishlistService.getAllWishlistsByUser(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public WishlistResponse getWishlistById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {

        return wishlistService.getWishListById(id, jwt.getSubject());
    }

    @GetMapping("/search")
    public List<WishlistResponse> getWishListByName(@RequestParam String name, @AuthenticationPrincipal Jwt jwt) {

        return wishlistService.getWishlistByName(name, jwt.getSubject());
    }

    @PutMapping("/{id}")
    public Response updateWishList(@PathVariable Long id, @Valid @RequestBody WishlistRequest wishlistDetails, @AuthenticationPrincipal Jwt jwt) {

        wishlistService.updateWishlist(id, wishlistDetails, jwt.getSubject());
        return new Response(
                WISHLIST_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{id}")
    public Response deleteWishListById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        wishlistService.deleteWishlist(id, jwt.getSubject());
        return new Response(
                WISHLIST_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}