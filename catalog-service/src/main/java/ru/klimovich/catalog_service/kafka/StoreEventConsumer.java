package ru.klimovich.catalog_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.service.impl.ProductBlockingFlags;
import ru.klimovich.catalog_service.service.impl.StoreProductsArchiver;
import ru.klimovich.catalog_service.service.impl.StoreProductsPurger;

/**
 * Потребитель store-events-topic. Значение — конверт
 * {aggregateType, eventType, payload}, где payload — JSON-строка события {storeId, sellerId}.
 * Ошибка разбора или обработки бросает исключение: его обрабатывает DefaultErrorHandler
 * (повторы и store-events-topic.DLT). Неизвестный eventType пропускается.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreEventConsumer {

    static final String TOPIC = "store-events-topic";
    static final String GROUP_ID = "catalog-group";

    private final StoreProductsArchiver archiver;
    private final StoreProductsPurger purger;
    private final ProductBlockingFlags flags;
    private final ObjectMapper objectMapper;

    @KafkaListener(id = "storeEventsListener", topics = TOPIC, groupId = GROUP_ID)
    public void consume(String message) {
        JsonNode envelope = readJson(message);
        String eventType = envelope.path("eventType").asText(null);
        if (eventType == null) {
            log.warn("Skipping store event without eventType");
            return;
        }

        switch (eventType) {
            // STORE_DELETED больше не публикуется; архивация остается для старых сообщений топика и DLT.
            case "STORE_DELETED" -> archiver.archiveProductsOfStore(storeId(envelope, eventType));
            case "STORE_PURGED" -> purger.purgeProductsOfStore(storeId(envelope, eventType));
            case "STORE_BLOCKED" -> flags.setStoreBlocked(storeId(envelope, eventType), true);
            case "STORE_UNBLOCKED" -> flags.setStoreBlocked(storeId(envelope, eventType), false);
            case "SELLER_BLOCKED" -> flags.setSellerBlocked(storeId(envelope, eventType), true);
            case "SELLER_UNBLOCKED" -> flags.setSellerBlocked(storeId(envelope, eventType), false);
            default -> log.warn("Skipping store event with unknown eventType {}", eventType);
        }
    }

    private long storeId(JsonNode envelope, String eventType) {
        JsonNode payload = readJson(envelope.path("payload").asText(""));
        JsonNode storeId = payload.get("storeId");
        if (storeId == null || !storeId.canConvertToLong()) {
            throw new IllegalArgumentException(eventType + " event without storeId");
        }
        return storeId.asLong();
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed store event", e);
        }
    }
}
