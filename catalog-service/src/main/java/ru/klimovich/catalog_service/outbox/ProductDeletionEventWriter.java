package ru.klimovich.catalog_service.outbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Записывает PRODUCT_DELETED в outbox ДО удаления документа товара (change product-deletion-sync, D2).
 * Транзакций MongoDB нет, поэтому публикатор отправит событие только если товара уже нет (D3).
 * Каждая операция атомарна над одним документом:
 * нет записи — создать PENDING; DISCARDED (прежнее удаление не прошло) — снова PENDING;
 * PENDING и SENT не меняются.
 */
@Component
public class ProductDeletionEventWriter {

    static final String ID_PREFIX = "PRODUCT_DELETED:";

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Autowired
    public ProductDeletionEventWriter(MongoTemplate mongoTemplate) {
        this(mongoTemplate, Clock.systemUTC());
    }

    public ProductDeletionEventWriter(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public void record(String productId, Long storeId) {
        String id = ID_PREFIX + productId;
        Instant now = Instant.now(clock);
        String eventId = UUID.randomUUID().toString();

        mongoTemplate.upsert(
                new Query(where("_id").is(id)),
                new Update()
                        .setOnInsert("eventId", eventId)
                        .setOnInsert("productId", productId)
                        .setOnInsert("storeId", storeId)
                        .setOnInsert("status", ProductOutboxStatus.PENDING)
                        .setOnInsert("createdAt", now),
                ProductOutboxEvent.class);

        mongoTemplate.updateFirst(
                new Query(where("_id").is(id).and("status").is(ProductOutboxStatus.DISCARDED)),
                new Update()
                        .set("status", ProductOutboxStatus.PENDING)
                        .set("eventId", eventId)
                        .set("createdAt", now),
                ProductOutboxEvent.class);
    }
}
