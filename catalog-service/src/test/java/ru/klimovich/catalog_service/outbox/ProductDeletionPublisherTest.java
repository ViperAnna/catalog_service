package ru.klimovich.catalog_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.klimovich.catalog_service.repository.ProductRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Change product-deletion-sync (D3): событие публикуется тогда и только тогда, когда товара в каталоге нет.
 */
@ExtendWith(MockitoExtension.class)
class ProductDeletionPublisherTest {

    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    @Mock private MongoTemplate mongoTemplate;
    @Mock private ProductRepository productRepo;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProductDeletionPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ProductDeletionPublisher(mongoTemplate, productRepo, kafkaTemplate, objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ProductOutboxEvent pending(String productId, Instant createdAt) {
        return new ProductOutboxEvent("PRODUCT_DELETED:" + productId, "ev-" + productId, productId, 3L,
                ProductOutboxStatus.PENDING, createdAt);
    }

    private void stubPending(ProductOutboxEvent... events) {
        when(mongoTemplate.find(any(Query.class), eq(ProductOutboxEvent.class))).thenReturn(List.of(events));
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<SendResult<String, String>> sent() {
        return CompletableFuture.completedFuture(null);
    }

    @Test
    @DisplayName("Товара нет: отправка с ключом productId и конвертом PRODUCT/PRODUCT_DELETED, затем SENT")
    void publishesWhenProductAbsent() throws Exception {
        stubPending(pending("p-1", NOW.minusSeconds(5)));
        when(productRepo.existsById("p-1")).thenReturn(false);
        when(kafkaTemplate.send(eq("product-events-topic"), eq("p-1"), anyString())).thenReturn(sent());

        publisher.publishEvents();

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("product-events-topic"), eq("p-1"), value.capture());
        JsonNode envelope = objectMapper.readTree(value.getValue());
        assertThat(envelope.get("aggregateType").asText()).isEqualTo("PRODUCT");
        assertThat(envelope.get("eventType").asText()).isEqualTo("PRODUCT_DELETED");
        JsonNode payload = objectMapper.readTree(envelope.get("payload").asText());
        assertThat(payload.get("productId").asText()).isEqualTo("p-1");
        assertThat(payload.get("storeId").asLong()).isEqualTo(3L);
        assertThat(payload.get("eventId").asText()).isEqualTo("ev-p-1");
        assertThat(payload.has("occurredAt")).isTrue();

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(ProductOutboxEvent.class));
        assertThat(query.getValue().getQueryObject().get("eventId")).isEqualTo("ev-p-1");
        assertThat(((Document) update.getValue().getUpdateObject().get("$set")).get("status"))
                .isEqualTo(ProductOutboxStatus.SENT);
    }

    @Test
    @DisplayName("Товар существует, запись моложе часа: событие не публикуется, запись не меняется")
    void skipsWhenProductStillExists() {
        stubPending(pending("p-1", NOW.minus(Duration.ofMinutes(59))));
        when(productRepo.existsById("p-1")).thenReturn(true);

        publisher.publishEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(UpdateDefinition.class), eq(ProductOutboxEvent.class));
    }

    @Test
    @DisplayName("Товар существует, запись старше часа: DISCARDED, событие не публикуется")
    void discardsWhenProductExistsTooLong() {
        stubPending(pending("p-1", NOW.minus(Duration.ofHours(1))));
        when(productRepo.existsById("p-1")).thenReturn(true);

        publisher.publishEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateFirst(any(Query.class), update.capture(), eq(ProductOutboxEvent.class));
        assertThat(((Document) update.getValue().getUpdateObject().get("$set")).get("status"))
                .isEqualTo(ProductOutboxStatus.DISCARDED);
    }

    @Test
    @DisplayName("Ошибка отправки: цикл прекращается, запись остается PENDING")
    void stopsOnSendFailure() {
        stubPending(pending("p-1", NOW.minusSeconds(5)), pending("p-2", NOW.minusSeconds(4)));
        when(productRepo.existsById("p-1")).thenReturn(false);
        when(kafkaTemplate.send(eq("product-events-topic"), eq("p-1"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));

        publisher.publishEvents();

        verify(kafkaTemplate, never()).send(eq("product-events-topic"), eq("p-2"), anyString());
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(UpdateDefinition.class), eq(ProductOutboxEvent.class));
    }
}
