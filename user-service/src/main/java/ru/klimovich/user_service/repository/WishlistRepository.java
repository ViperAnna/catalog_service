package ru.klimovich.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.user_service.model.Wishlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

//    List<Wishlist> findByUserId(String userId);

    Optional<Wishlist> findByIdAndKeycloakUserId(Long id, String keycloakUserId);

    List<Wishlist> findByKeycloakUserId(String keycloakUserId);

    boolean existsByKeycloakUserIdAndName(String keycloakUserId, String name);

    List<Wishlist> findByKeycloakUserIdAndNameContainingIgnoreCase(String keycloakUserId, String name);

    Optional<Wishlist> findById(Long id);

//    Optional<Wishlist> findByName(String name);
}
