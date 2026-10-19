package ru.klimovich.order_service.domain;

import ru.klimovich.order_service.model.OrderStatus;

import java.util.Map;
import java.util.Set;

/**
 * Таблица допустимых переходов статуса заказа (design.md D6). Единственная точка, определяющая,
 * можно ли изменить статус; все остальные переходы, включая повторную отмену, запрещены.
 */
public final class OrderTransitions {

    private record Transition(OrderStatus from, OrderStatus to) {
    }

    private static final Map<Transition, Set<OrderInitiator>> ALLOWED = Map.of(
            new Transition(OrderStatus.CREATED, OrderStatus.CANCELLED), Set.of(OrderInitiator.CUSTOMER)
    );

    private OrderTransitions() {
    }

    public static boolean isAllowed(OrderStatus from, OrderStatus to, OrderInitiator initiator) {
        return ALLOWED.getOrDefault(new Transition(from, to), Set.of()).contains(initiator);
    }
}
