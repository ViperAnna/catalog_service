package ru.klimovich.catalog_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.service.impl.StoreProductsArchiver;

/**
 * Потребитель store-events-topic. Значение — конверт
 * {aggregateType, eventType, payload}, где payload — JSON-строка события.
 * Ошибка разбора бросает исключение: его обрабатывает DefaultErrorHandler
 * (повторы и store-events-topic.DLT).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreEventConsumer {

    static final String TOPIC = "store-events-topic";
    static final String GROUP_ID = "catalog-group";
    private static final String STORE_DELETED = "STORE_DELETED";

    private final StoreProductsArchiver archiver;
    private final ObjectMapper objectMapper;

    @KafkaListener(id = "storeEventsListener", topics = TOPIC, groupId = GROUP_ID)
    public void consume(String message) {
        JsonNode envelope = readJson(message);
        String eventType = envelope.path("eventType").asText(null);

        if (!STORE_DELETED.equals(eventType)) {
            log.warn("Skipping store event with unknown eventType {}", eventType);
            return;
        }

        JsonNode payload = readJson(envelope.path("payload").asText(""));
        JsonNode storeId = payload.get("storeId");
        if (storeId == null || !storeId.canConvertToLong()) {
            throw new IllegalArgumentException("STORE_DELETED event without storeId");
        }
        archiver.archiveProductsOfStore(storeId.asLong());
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed store event", e);
        }
    }
}
