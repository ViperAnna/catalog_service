package ru.klimovich.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Конверт order-events-topic. eventType — строка (в отличие от KafkaEvent seller-событий),
 * чтобы неизвестный тип классифицировался явно, а не падал при десериализации.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderKafkaEvent {
    private String aggregateType;
    private String eventType;
    private String payload;
}
