package ru.klimovich.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.klimovich.order_service.model.Cart;
import ru.klimovich.order_service.model.CartItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartOrderByCreatedAtAscIdAsc(Cart cart);

    Optional<CartItem> findByCartAndProductId(Cart cart, String productId);

    long countByCart(Cart cart);

    @Modifying
    @Query("delete from CartItem i where i.cart = :cart")
    int deleteAllByCart(@Param("cart") Cart cart);
}
