package ru.klimovich.order_service.domain;

import ru.klimovich.order_service.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Построение события заказа и его обезличенной формы (design.md D8, D9).
 */
public final class OrderEventFactory {

    public static final String AGGREGATE_TYPE = "ORDER";
    private static final String EVENT_TYPE_PREFIX = "ORDER_";

    private OrderEventFactory() {
    }

    /** ORDER_CREATED, ORDER_CANCELLED: тип события соответствует новому статусу заказа. */
    public static String eventType(OrderStatus status) {
        return EVENT_TYPE_PREFIX + status.name();
    }

    public static OrderEventPayload payload(UUID eventId,
                                            LocalDateTime occurredAt,
                                            UUID orderId,
                                            String keycloakUserId,
                                            OrderStatus status,
                                            BigDecimal totalAmount,
                                            int itemsCount,
                                            String verifiedEmail) {
        return new OrderEventPayload(eventId, occurredAt, orderId, keycloakUserId,
                status.name(), totalAmount, itemsCount, verifiedEmail);
    }

    /** Payload записи outbox после подтвержденной публикации: без email и прочих данных. */
    public static Map<String, Object> redactedPayload(String orderId, UUID eventId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("eventId", eventId);
        payload.put("redacted", true);
        return payload;
    }
}
