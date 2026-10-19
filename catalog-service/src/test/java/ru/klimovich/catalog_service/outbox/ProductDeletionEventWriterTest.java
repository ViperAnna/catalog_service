package ru.klimovich.catalog_service.outbox;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Change product-deletion-sync (D2): запись PRODUCT_DELETED в outbox — атомарный upsert одного документа.
 */
@ExtendWith(MockitoExtension.class)
class ProductDeletionEventWriterTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private ProductDeletionEventWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ProductDeletionEventWriter(mongoTemplate,
                Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Новая запись: id PRODUCT_DELETED:<productId>, PENDING только при вставке (существующая не меняется)")
    void insertsPendingOnlyIfAbsent() {
        writer.record("p-1", 3L);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).upsert(query.capture(), update.capture(), eq(ProductOutboxEvent.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("_id", "PRODUCT_DELETED:p-1"));
        Document onInsert = (Document) update.getValue().getUpdateObject().get("$setOnInsert");
        assertThat(onInsert.get("productId")).isEqualTo("p-1");
        assertThat(onInsert.get("storeId")).isEqualTo(3L);
        assertThat(onInsert.get("status")).isEqualTo(ProductOutboxStatus.PENDING);
        assertThat(onInsert).containsKeys("eventId", "createdAt");
        assertThat(update.getValue().getUpdateObject()).doesNotContainKey("$set");
    }

    @Test
    @DisplayName("Отброшенная запись (неудачное прежнее удаление) снова ставится в очередь")
    void requeuesDiscarded() {
        writer.record("p-1", 3L);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(ProductOutboxEvent.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(new Document("_id", "PRODUCT_DELETED:p-1").append("status", ProductOutboxStatus.DISCARDED));
        Document set = (Document) update.getValue().getUpdateObject().get("$set");
        assertThat(set.get("status")).isEqualTo(ProductOutboxStatus.PENDING);
        assertThat(set).containsKeys("eventId", "createdAt");
    }
}
