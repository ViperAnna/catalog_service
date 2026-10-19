package ru.klimovich.order_service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload событий order-events-topic (spec integration-events "order-events-topic message contract").
 * Персональные данные — только keycloakUserId и email (design.md D9).
 */
public record OrderEventPayload(UUID eventId,
                                LocalDateTime occurredAt,
                                UUID orderId,
                                String keycloakUserId,
                                String status,
                                BigDecimal totalAmount,
                                int itemsCount,
                                String email) {
}
