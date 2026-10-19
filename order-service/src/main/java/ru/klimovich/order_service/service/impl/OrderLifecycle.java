package ru.klimovich.order_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.order_service.domain.OrderInitiator;
import ru.klimovich.order_service.domain.OrderTransitions;
import ru.klimovich.order_service.exception.ResourceConflictException;
import ru.klimovich.order_service.model.Order;
import ru.klimovich.order_service.model.OrderStatus;
import ru.klimovich.order_service.outbox.OrderEventWriter;
import ru.klimovich.order_service.util.MessageKeys;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Единственная точка изменения статуса заказа (design.md D6): проверка перехода по
 * OrderTransitions, изменение состояния и запись события ORDER_<STATUS> в той же транзакции.
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OrderLifecycle {

    private final OrderEventWriter orderEventWriter;
    private final Clock clock;

    /** Новый заказ: статус CREATED и событие ORDER_CREATED. Заказ должен быть уже сохранен. */
    public void created(Order order, String verifiedEmail) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("New order must be in status CREATED");
        }
        orderEventWriter.write(order, verifiedEmail);
    }

    /**
     * Переход в target. Недопустимый переход (в том числе повторная отмена) — 409 без изменений и событий.
     */
    public void transition(Order order, OrderStatus target, OrderInitiator initiator, String verifiedEmail) {
        OrderStatus current = order.getStatus();
        if (!OrderTransitions.isAllowed(current, target, initiator)) {
            throw new ResourceConflictException(MessageKeys.ORDER_TRANSITION_NOT_ALLOWED, order.getId(), current, target);
        }
        order.setStatus(target);
        if (target == OrderStatus.CANCELLED) {
            order.setCancelledAt(LocalDateTime.now(clock));
        }
        orderEventWriter.write(order, verifiedEmail);
    }
}
