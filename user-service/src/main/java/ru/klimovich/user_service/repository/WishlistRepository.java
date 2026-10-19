package ru.klimovich.user_service.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.model.Wishlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByKeycloakUserId(String keycloakUserId);

    Optional<Wishlist> findByIdAndKeycloakUserId(Long id, String keycloakUserId);

    /**
     * Вишлист с блокировкой строки (SELECT ... FOR UPDATE): добавления в один вишлист
     * выполняются по очереди, лимит товаров не превышается при гонке.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wishlist w where w.id = :id and w.keycloakUserId = :keycloakUserId")
    Optional<Wishlist> findByIdAndKeycloakUserIdForUpdate(@Param("id") Long id,
                                                          @Param("keycloakUserId") String keycloakUserId);

    long countByKeycloakUserId(String keycloakUserId);

    /**
     * Удаляет id физически удаленного товара из всех вишлистов (событие PRODUCT_DELETED,
     * change product-deletion-sync). Одна команда, повтор ничего не меняет.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM wishlist_products WHERE product_id = :productId", nativeQuery = true)
    int deleteProductFromAllWishlists(@Param("productId") String productId);

    /**
     * Транзакционная advisory-блокировка по ключу: создания вишлистов одного пользователя
     * выполняются по очереди, лимит вишлистов не превышается при гонке. Снимается при завершении транзакции.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:key))) AS lock", nativeQuery = true)
    Integer lockUserWishlists(@Param("key") String key);




    /** Имя сравнивается без учета регистра; пробелы по краям обрезает сервис до вызова. */
    boolean existsByKeycloakUserIdAndNameIgnoreCase(String keycloakUserId, String name);

    boolean existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot(String keycloakUserId, String name, Long id);







}
