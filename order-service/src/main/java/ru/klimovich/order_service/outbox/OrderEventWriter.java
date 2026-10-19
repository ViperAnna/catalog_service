package ru.klimovich.order_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.order_service.domain.OrderEventFactory;
import ru.klimovich.order_service.domain.OrderEventPayload;
import ru.klimovich.order_service.mapper.EventMapper;
import ru.klimovich.order_service.model.Order;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Записывает событие заказа в outbox в текущей транзакции (design.md D8).
 * id записи = eventId события; он не меняется при повторных публикациях.
 * Ключ сообщения — orderId, поэтому события одного заказа публикуются по порядку.
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OrderEventWriter {

    private final OutboxRepository outboxRepo;
    private final EventMapper eventMapper;
    private final Clock clock;

    public void write(Order order, String verifiedEmail) {
        UUID eventId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.now(clock);
        OrderEventPayload payload = OrderEventFactory.payload(
                eventId,
                occurredAt,
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getItemsCount(),
                verifiedEmail
        );
        outboxRepo.save(
                OutboxEvent.builder()
                        .id(eventId)
                        .aggregateType(OrderEventFactory.AGGREGATE_TYPE)
                        .aggregateId(order.getId().toString())
                        .eventType(OrderEventFactory.eventType(order.getStatus()))
                        .payload(eventMapper.toJson(payload))
                        .createdAt(occurredAt)
                        .processed(false)
                        .build()
        );
    }
}
