package ru.klimovich.order_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.klimovich.order_service.model.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserId(String userId);

    /**
     * Блокировка строки корзины (SELECT ... FOR UPDATE): сериализует изменения корзины
     * и оформление заказа одного пользователя.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.userId = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") String userId);

    /**
     * Ленивое создание корзины без гонки: параллельная вставка того же user_id ничего не делает.
     */
    @Modifying
    @Query(value = """
            INSERT INTO carts (id, user_id, created_at, updated_at)
            VALUES (gen_random_uuid(), :userId, now(), now())
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") String userId);
}
