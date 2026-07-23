package ru.klimovich.user_service.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.user_service.model.Wishlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = "items")
    List<Wishlist> findByKeycloakUserId(String keycloakUserId);

    @EntityGraph(attributePaths = "items")
    Optional<Wishlist> findByIdAndKeycloakUserId(Long id, String keycloakUserId);

    boolean existsByKeycloakUserIdAndName(String keycloakUserId, String name);

    boolean existsByKeycloakUserIdAndNameAndIdNot(String keycloakUserId, String name, Long id);

    List<Wishlist> findByKeycloakUserIdAndNameContainingIgnoreCase(String keycloakUserId, String name);

    Optional<Wishlist> findByName(String name);
}
