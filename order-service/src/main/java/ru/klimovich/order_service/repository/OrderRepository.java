package ru.klimovich.order_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.klimovich.order_service.model.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Все выборки — только в пределах владельца (user_id = sub).
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndUserId(UUID id, String userId);

    Page<Order> findByUserId(String userId, Pageable pageable);
}
