package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.client.CatalogClient;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.dto.responce.WishlistShortResponse;
import ru.klimovich.user_service.exception.ResourceConflictException;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.exception.WishlistUniqueViolationTranslator;
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
    private final CatalogClient catalogClient;

    static final int MAX_WISHLISTS_PER_USER = 20;
    static final int MAX_PRODUCTS_PER_WISHLIST = 200;

    @Override
    public WishlistResponse createWishlist(WishlistRequest wishlistDetails) {
        String userId = currentUserService.getUserId();
        String name = normalizeName(wishlistDetails.getName());

        // Создания вишлистов одного пользователя выполняются по очереди: лимит не превышается при гонке.
        wishlistRepo.lockUserWishlists("wishlists:" + userId);
        if (wishlistRepo.countByKeycloakUserId(userId) >= MAX_WISHLISTS_PER_USER) {
            throw new ResourceConflictException(MessageKeys.WISHLIST_LIMIT_REACHED, MAX_WISHLISTS_PER_USER);
        }
        validateUniqueName(userId, name, null);

        Wishlist wishlist = wishlistMapper.toEntity(wishlistDetails);
        wishlist.setName(name);
        wishlist.setKeycloakUserId(userId);

        return wishlistMapper.toDTO(saveAndFlush(wishlist, name, null));
    }

    @Override
    public List<WishlistShortResponse> getAllWishlistsByUser() {
        String keycloakUserId = currentUserService.getUserId();

        return wishlistRepo.findByKeycloakUserId(keycloakUserId)
                .stream()
                .map(wishlist -> {
                    WishlistShortResponse response = wishlistMapper.toShortDTO(wishlist);
                    response.setProductCount(wishlist.getProductIds().size());
                    return response;
                })

                .toList();
    }

    @Override
    public WishlistResponse getWishListById(Long wishlistId) {
        Wishlist wishlist = getWishlist(wishlistId);
        return responseBuilder.buildWishlistResponse(wishlist, currentUserService.getAccessToken());
    }

    @Override
    public WishlistResponse addProduct(Long wishlistId, String productId) {
        String accessToken = currentUserService.getAccessToken();

        getWishlist(wishlistId);
        catalogClient.getProductById(productId, accessToken);

        // Блокировка после вызова каталога, чтобы не держать ее на время сетевого запроса.
        Wishlist wishlist = wishlistRepo
                .findByIdAndKeycloakUserIdForUpdate(wishlistId, currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(MessageKeys.WISHLIST_NOT_FOUND, wishlistId)));

        if (wishlist.getProductIds().contains(productId)) {
            throw new ResourceConflictException(MessageKeys.PRODUCT_ALREADY_IN_WISHLIST, productId);
        }
        if (wishlist.getProductIds().size() >= MAX_PRODUCTS_PER_WISHLIST) {
            throw new ResourceConflictException(MessageKeys.WISHLIST_PRODUCT_LIMIT_REACHED, MAX_PRODUCTS_PER_WISHLIST);
        }
        wishlist.getProductIds().add(productId);

        saveAndFlush(wishlist, wishlist.getName(), productId);
        return responseBuilder.buildWishlistResponse(wishlist, accessToken);
    }

    private Wishlist getWishlist(Long wishlistId) {
        return wishlistRepo
                .findByIdAndKeycloakUserId(wishlistId, currentUserService.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                String.format(MessageKeys.WISHLIST_NOT_FOUND, wishlistId)
                        ));
    }


    @Override
    public WishlistResponse removeProduct(Long wishlistId, String productId) {
        String accessToken = currentUserService.getAccessToken();
        Wishlist wishlist = getWishlist(wishlistId);

        if (!wishlist.getProductIds().remove(productId)) {
            throw new ResourceNotFoundException(MessageKeys.WISHLIST_ITEM_NOT_FOUND, productId);
        }
        // Удаление записывается сразу и не зависит от каталога: при его недоступности ответ без данных товаров.
        wishlistRepo.saveAndFlush(wishlist);
        return responseBuilder.buildWishlistResponseOrWithoutProducts(wishlist, accessToken);
    }

    @Override
    public WishlistResponse updateWishlist(Long wishlistId, WishlistRequest wishlistDetails) {
        String userId = currentUserService.getUserId();
        String accessToken = currentUserService.getAccessToken();

        // Сначала существование и владение (404), затем уникальность имени (409).
        Wishlist wishlist = getWishlist(wishlistId);

        String name = normalizeName(wishlistDetails.getName());
        validateUniqueName(userId, name, wishlistId);

        wishlistMapper.updateFromDTO(wishlistDetails, wishlist);
        wishlist.setName(name);
        saveAndFlush(wishlist, name, null);

        return responseBuilder.buildWishlistResponse(wishlist, accessToken);
    }

    /**
     * Запись с немедленной проверкой ограничений БД: нарушение уникального индекса при гонке
     * дает 409 с тем же сообщением, что проверка в коде, а не 500 при commit.
     */
    private Wishlist saveAndFlush(Wishlist wishlist, String wishlistName, String productId) {
        try {
            return wishlistRepo.saveAndFlush(wishlist);
        } catch (DataIntegrityViolationException e) {
            throw WishlistUniqueViolationTranslator
                    .translate(e, wishlistName, productId)
                    .orElseThrow(() -> e);
        }
    }

    /** Пробелы по краям не входят в имя вишлиста. */
    private static String normalizeName(String name) {
        return name == null ? null : name.strip();
    }

    /** Уникальность имени в пределах пользователя без учета регистра (имя уже обрезано). */
    private void validateUniqueName(String userId, String wishlistName, Long wishlistId) {
        boolean exists = wishlistId == null
                ? wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCase(userId, wishlistName)
                : wishlistRepo.existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(
                userId,
                wishlistName,
                wishlistId
        );
        if (exists) {
            throw new ResourceConflictException(
                    String.format(
                            MessageKeys.WISHLIST_NAME_ALREADY_EXIST,
                            wishlistName
                    )
            );
        }
    }

    @Override
    public void deleteWishlist(Long wishlistId) {
        wishlistRepo.delete(getWishlist(wishlistId));
    }
}
