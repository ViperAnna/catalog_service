package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
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

    @Override
    public WishlistResponse createWishlist(WishlistRequest wishlistDetails) {
        String userId = currentUserService.getUserId();
        validateUniqueName(userId, wishlistDetails.getName(), null);

        Wishlist wishlist = wishlistMapper.toEntity(wishlistDetails);
        wishlist.setKeycloakUserId(userId);

        return wishlistMapper.toDTO(wishlistRepo.save(wishlist));
    }

    @Override
    public List<WishlistResponse> getAllWishlistsByUser() {
        String keycloakUserId = currentUserService.getUserId();
        String accessToken = currentUserService.getAccessToken();

        return wishlistRepo.findByKeycloakUserId(keycloakUserId)
                .stream()
                .map(wishlist ->
                        responseBuilder.buildWishlistResponse(wishlist, accessToken))
                .toList();
    }

    private Wishlist getWishlist(Long id) {
        return wishlistRepo
                .findByIdAndKeycloakUserId(id, currentUserService.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                String.format(MessageKeys.WISHLIST_NOT_FOUND, id)
                        ));
    }

    @Override
    public WishlistResponse getWishListById(Long id) {
        Wishlist wishlist = getWishlist(id);
        return responseBuilder.buildWishlistResponse(wishlist, currentUserService.getAccessToken());
    }

    @Override
    public WishlistResponse updateWishlist(Long id, WishlistRequest wishlistDetails) {
        String userId = currentUserService.getUserId();

        validateUniqueName(userId, wishlistDetails.getName(), id);

        Wishlist wishlist = getWishlist(id);

        wishlistMapper.updateUserFromDTO(wishlistDetails, wishlist);

        return responseBuilder.buildWishlistResponse(
                wishlistRepo.save(wishlist), currentUserService.getAccessToken());
    }

    private void validateUniqueName(String userId, String name, Long wishlistId) {
        boolean exists = wishlistId == null
                ? wishlistRepo.existsByKeycloakUserIdAndName(userId, name)
                : wishlistRepo.existsByKeycloakUserIdAndNameAndIdNot(
                userId,
                name,
                wishlistId
        );
        if (exists) {
            throw new ResourceConflictException(
                    String.format(
                            MessageKeys.WISHLIST_NAME_ALREADY_EXIST,
                            name
                    )
            );
        }
    }

    @Override
    public void deleteWishlist(Long id) {
        wishlistRepo.delete(getWishlist(id));
    }
}
