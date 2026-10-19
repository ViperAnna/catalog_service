package ru.klimovich.catalog_service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.repository.ProductRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static ru.klimovich.catalog_service.config.ProductEventsTopicConfig.PRODUCT_EVENTS_TOPIC;

/**
 * Публикация PRODUCT_DELETED из outbox (change product-deletion-sync, D3). Событие уходит тогда и только
 * тогда, когда документа товара в каталоге нет: товар не может появиться снова (ObjectId не переиспользуется),
 * поэтому проверка «отсутствует» не устаревает. Если товар существует час и дольше после записи — удаление
 * не прошло, запись отбрасывается. Ошибка отправки прекращает цикл (порядок по createdAt).
 */
@Slf4j
@Component
public class ProductDeletionPublisher {

    static final Duration DISCARD_AFTER = Duration.ofHours(1);
    private static final int BATCH_SIZE = 100;
    private static final String AGGREGATE_TYPE = "PRODUCT";
    private static final String EVENT_TYPE = "PRODUCT_DELETED";

    private final MongoTemplate mongoTemplate;
    private final ProductRepository productRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ProductDeletionPublisher(MongoTemplate mongoTemplate,
                                    ProductRepository productRepo,
                                    KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper) {
        this(mongoTemplate, productRepo, kafkaTemplate, objectMapper, Clock.systemUTC());
    }

    public ProductDeletionPublisher(MongoTemplate mongoTemplate,
                                    ProductRepository productRepo,
                                    KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper,
                                    Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.productRepo = productRepo;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedRate = 3000)
    public void publishEvents() {
        List<ProductOutboxEvent> pending = mongoTemplate.find(
                new Query(where("status").is(ProductOutboxStatus.PENDING))
                        .with(Sort.by(Sort.Direction.ASC, "createdAt"))
                        .limit(BATCH_SIZE),
                ProductOutboxEvent.class);

        for (ProductOutboxEvent event : pending) {
            if (productRepo.existsById(event.getProductId())) {
                discardIfExpired(event);
                continue;
            }
            try {
                kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, event.getProductId(), envelope(event))
                        .get(10, TimeUnit.SECONDS);
                markStatus(event, ProductOutboxStatus.SENT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Publishing interrupted for outbox event {}", event.getId());
                return;
            } catch (ExecutionException | TimeoutException e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getClass().getSimpleName());
                return;
            }
        }
    }

    /**
     * Товар все еще существует: удаление не прошло. Через час событие отбрасывается;
     * повторное удаление того же товара вернет запись в PENDING (ProductDeletionEventWriter).
     */
    private void discardIfExpired(ProductOutboxEvent event) {
        if (event.getCreatedAt().plus(DISCARD_AFTER).isAfter(Instant.now(clock))) {
            return;
        }
        log.error("Product {} still exists {} after deletion event {} was recorded; event discarded",
                event.getProductId(), DISCARD_AFTER, event.getId());
        markStatus(event, ProductOutboxStatus.DISCARDED);
    }

    /**
     * Меняет статус только той же попытки (eventId) и только из PENDING: повторное удаление,
     * вернувшее запись в очередь с новым eventId, не перезаписывается.
     */
    private void markStatus(ProductOutboxEvent event, ProductOutboxStatus status) {
        mongoTemplate.updateFirst(
                new Query(where("_id").is(event.getId())
                        .and("eventId").is(event.getEventId())
                        .and("status").is(ProductOutboxStatus.PENDING)),
                new Update().set("status", status),
                ProductOutboxEvent.class);
    }

    private String envelope(ProductOutboxEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", event.getEventId());
        payload.put("occurredAt", event.getCreatedAt().toString());
        payload.put("productId", event.getProductId());
        payload.put("storeId", event.getStoreId());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("aggregateType", AGGREGATE_TYPE);
        envelope.put("eventType", EVENT_TYPE);
        envelope.put("payload", toJson(payload));
        return toJson(envelope);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize product event", e);
        }
    }
}
