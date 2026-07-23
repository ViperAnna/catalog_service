package ru.klimovich.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.user_service.model.WishlistItem;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    boolean existsByWishlistIdAndProductId(Long wishlist, String productId);

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlist, String productId);
}
