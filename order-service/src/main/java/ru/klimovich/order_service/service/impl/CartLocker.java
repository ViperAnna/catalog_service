package ru.klimovich.order_service.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.order_service.model.Cart;
import ru.klimovich.order_service.repository.CartRepository;

import java.util.Optional;

/**
 * Блокировка строки корзины в текущей транзакции (design.md D4). Ожидание блокировки ограничено
 * lock_timeout = 5 с; при превышении PostgreSQL отменяет запрос, и ошибка превращается в 409.
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CartLocker {

    private static final String SET_LOCK_TIMEOUT = "SET LOCAL lock_timeout = '5s'";

    private final EntityManager entityManager;
    private final CartRepository cartRepo;

    /** Блокирует существующую корзину пользователя; корзину не создает. */
    public Optional<Cart> lock(String userId) {
        setLockTimeout();
        return cartRepo.findByUserIdForUpdate(userId);
    }

    /** Создает корзину при отсутствии (без гонки) и блокирует ее. */
    public Cart lockOrCreate(String userId) {
        setLockTimeout();
        cartRepo.insertIfAbsent(userId);
        return cartRepo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Cart was not created"));
    }

    private void setLockTimeout() {
        entityManager.createNativeQuery(SET_LOCK_TIMEOUT).executeUpdate();
    }
}
