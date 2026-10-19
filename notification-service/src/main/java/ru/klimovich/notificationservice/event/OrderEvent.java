package ru.klimovich.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload событий заказа (integration-events "order-events-topic message contract").
 * email не логируется и не сохраняется.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEvent {
    private UUID eventId;
    private LocalDateTime occurredAt;
    private UUID orderId;
    private String keycloakUserId;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemsCount;
    private String email;
}
