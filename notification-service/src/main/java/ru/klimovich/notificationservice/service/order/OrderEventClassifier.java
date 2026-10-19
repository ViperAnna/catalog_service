package ru.klimovich.notificationservice.service.order;

import java.util.Set;

/**
 * Классы событий заказа (design.md D10 change add-order-service).
 * Новый тип order-service сначала добавляется сюда — в NOTIFIABLE или NOTIFICATION_DISABLED,
 * иначе сообщение уходит в DLT как неизвестное.
 */
public final class OrderEventClassifier {

    public enum OrderEventClass {
        ORDER_CREATED,
        ORDER_CANCELLED,
        /** Известное событие, для которого уведомление не предусмотрено. */
        SKIPPED_BY_POLICY,
        UNKNOWN
    }

    /** Известные события без уведомления. В MVP пусто. */
    static final Set<String> NOTIFICATION_DISABLED = Set.of();

    private OrderEventClassifier() {
    }

    public static OrderEventClass classify(String eventType) {
        if ("ORDER_CREATED".equals(eventType)) {
            return OrderEventClass.ORDER_CREATED;
        }
        if ("ORDER_CANCELLED".equals(eventType)) {
            return OrderEventClass.ORDER_CANCELLED;
        }
        if (eventType != null && NOTIFICATION_DISABLED.contains(eventType)) {
            return OrderEventClass.SKIPPED_BY_POLICY;
        }
        return OrderEventClass.UNKNOWN;
    }
}
