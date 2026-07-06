package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.exception.ResourceConflictException;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.mapper.ItemMapper;
import ru.klimovich.user_service.mapper.WishlistMapper;
import ru.klimovich.user_service.model.Wishlist;
import ru.klimovich.user_service.repository.WishlistRepository;
import ru.klimovich.user_service.service.WishlistService;
import ru.klimovich.user_service.util.MessageKeys;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {
    private final WishlistRepository wishlistRepo;
    private final WishlistMapper wishlistMapper;
    public final ItemMapper itemMapper;

    @Override
    public WishlistResponse createWishlist(WishlistRequest wishlistDetails, String keycloakUserId) {

//        User user = userRepo.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        String.format(MessageKeys.USER_NOT_FOUND, userId)
//                ));

        if (wishlistRepo.existsByKeycloakUserIdAndName(keycloakUserId, wishlistDetails.getName())) {
            throw new ResourceConflictException(String.format(
                    MessageKeys.WISHLIST_NAME_ALREADY_EXIST, wishlistDetails.getName()));
        }
        Wishlist wishlist = new Wishlist();
        wishlist.setName(wishlistDetails.getName());
        wishlist.setKeycloakUserId(keycloakUserId);

        return wishlistMapper.toDTO(wishlistRepo.save(wishlist));
    }


    @Override
    public List<WishlistResponse> getAllWishlistsByUser(String keycloakUserId) {

//        return wishlistRepo.findAll()
//                .stream()
//                .map(wishlistMapper::toDTO)
//                .toList();

        return wishlistRepo.findByKeycloakUserId(keycloakUserId)
                .stream()
                .map(wishlistMapper::toDTO)
                .toList();
    }


    public List<WishlistResponse> getWishlistsByUser(String keycloakUserId) {
//
//        return wishlistRepo.findByUserId(userId)
//                .stream()
//                .map(wishlistMapper::toDTO)
//                .toList();

        return wishlistRepo.findByKeycloakUserId(keycloakUserId)
                .stream()
                .map(wishlistMapper::toDTO)
                .toList();
    }


    @Override
    public WishlistResponse getWishListById(Long id, String keycloakUserId) {

        Wishlist wishlist = wishlistRepo.findByIdAndKeycloakUserId(id, keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(MessageKeys.WISHLIST_NOT_FOUND, id)
                ));

        return wishlistMapper.toDTO(wishlist);
    }

    @Override
    public List<WishlistResponse> getWishlistByName(String name, String keycloakUserId) {

        return wishlistRepo.findByKeycloakUserIdAndNameContainingIgnoreCase(keycloakUserId, name)
                .stream()
                .map(wishlistMapper::toDTO)
                .toList();
    }

    @Override
    public WishlistResponse updateWishlist(Long id, WishlistRequest wishlistDetails, String keycloakUserId) {

        Wishlist wishlist = wishlistRepo.findByIdAndKeycloakUserId(id, keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(MessageKeys.WISHLIST_NOT_FOUND, id)
                ));

        wishlist.setName(wishlistDetails.getName());

        return wishlistMapper.toDTO(wishlistRepo.save(wishlist));
    }

    @Override
    public void deleteWishlist(Long id, String keycloakUserId) {

        Wishlist wishlist = wishlistRepo.findByIdAndKeycloakUserId(id, keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Wishlist not found: %d", id)
                ));

        wishlistRepo.delete(wishlist);
    }
}
