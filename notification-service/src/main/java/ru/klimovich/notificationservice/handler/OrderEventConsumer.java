package ru.klimovich.notificationservice.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.klimovich.notificationservice.event.OrderEvent;
import ru.klimovich.notificationservice.event.OrderKafkaEvent;
import ru.klimovich.notificationservice.exception.InvalidOrderEventException;
import ru.klimovich.notificationservice.service.order.OrderEventClassifier;
import ru.klimovich.notificationservice.service.order.OrderEventClassifier.OrderEventClass;
import ru.klimovich.notificationservice.service.order.OrderNotificationService;

/**
 * Потребитель order-events-topic. Неизвестные и некорректные сообщения не пропускаются молча:
 * ERROR в лог (topic, partition, offset, eventType, причина) и InvalidOrderEventException,
 * которую KafkaConfig отправляет в order-events-topic.DLT без повторов.
 * Значение сообщения и email в лог не пишутся.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String ORDER_AGGREGATE = "ORDER";

    private final ObjectMapper objectMapper;
    private final OrderNotificationService notificationService;

    @KafkaListener(
            topics = "order-events-topic",
            groupId = "notification-group"
    )
    public void consume(ConsumerRecord<String, String> record) {
        OrderKafkaEvent envelope = read(record, record.value(), OrderKafkaEvent.class, null, "message is not a valid JSON envelope");
        String eventType = envelope.getEventType();

        if (eventType == null || eventType.isBlank()) {
            throw reject(record, null, "envelope has no eventType");
        }
        if (!ORDER_AGGREGATE.equals(envelope.getAggregateType())) {
            throw reject(record, eventType, "aggregateType is not ORDER");
        }
        if (envelope.getPayload() == null || envelope.getPayload().isBlank()) {
            throw reject(record, eventType, "envelope has no payload");
        }

        OrderEventClass eventClass = OrderEventClassifier.classify(eventType);
        if (eventClass == OrderEventClass.UNKNOWN) {
            throw reject(record, eventType, "unknown eventType");
        }

        OrderEvent event = read(record, envelope.getPayload(), OrderEvent.class, eventType, "payload is not valid JSON");
        if (event.getEventId() == null || event.getOrderId() == null) {
            throw reject(record, eventType, "payload has no eventId or orderId");
        }

        notificationService.process(event, eventType, eventClass);
    }

    /**
     * Ошибка разбора не прикладывается как cause: ее текст может содержать фрагмент сообщения.
     */
    private <T> T read(ConsumerRecord<String, String> record, String json, Class<T> type,
                       String eventType, String reason) {
        if (json == null) {
            throw reject(record, eventType, reason);
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw reject(record, eventType, reason);
        }
    }

    private static InvalidOrderEventException reject(ConsumerRecord<String, String> record, String eventType, String reason) {
        log.error("Rejected order event {}-{}@{} (eventType {}): {}, sending to DLT",
                record.topic(), record.partition(), record.offset(), eventType, reason);
        return new InvalidOrderEventException(reason);
    }
}
