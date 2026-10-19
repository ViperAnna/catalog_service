package ru.klimovich.order_service.outbox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Конверт сообщения Kafka того же формата, что в seller-events-topic: payload — JSON-строка.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEvent {
    private String aggregateType;
    private String eventType;
    private String payload;
}
