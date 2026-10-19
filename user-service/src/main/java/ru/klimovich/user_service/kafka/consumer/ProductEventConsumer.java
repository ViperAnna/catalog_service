package ru.klimovich.user_service.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.klimovich.user_service.repository.WishlistRepository;

/**
 * Потребитель product-events-topic (change product-deletion-sync, D4). Конверт {aggregateType, eventType, payload},
 * payload — JSON-строка {eventId, occurredAt, productId, storeId}. PRODUCT_DELETED удаляет id товара из всех
 * вишлистов (идемпотентно). Неизвестный eventType пропускается; ошибка разбора — исключение для повторов и DLT.
 * Архивные и заблокированные товары событий не создают и из вишлистов не удаляются.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    static final String TOPIC = "product-events-topic";
    static final String GROUP_ID = "user-service-group";
    private static final String PRODUCT_DELETED = "PRODUCT_DELETED";

    private final WishlistRepository wishlistRepo;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public void consume(String message) {
        JsonNode envelope = readJson(message);
        String eventType = envelope.path("eventType").asText(null);

        if (!PRODUCT_DELETED.equals(eventType)) {
            log.warn("Skipping product event with eventType {}", eventType);
            return;
        }

        JsonNode payload = readJson(envelope.path("payload").asText(""));
        String productId = payload.path("productId").asText(null);
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("PRODUCT_DELETED event without productId");
        }

        int removed = wishlistRepo.deleteProductFromAllWishlists(productId);
        log.info("Product {} deleted from catalog: removed from {} wishlist entries", productId, removed);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed product event", e);
        }
    }
}
