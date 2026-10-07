package ru.seller_service.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.seller_service.mapper.EventMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Requirement "Outbox publishing to seller-events-topic" (seller-onboarding) после
 * change archive-products-of-deleted-store: маршрутизация по aggregateType.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String SELLER_TOPIC = "seller-events-topic";
    private static final String STORE_TOPIC = "store-events-topic";

    @Mock
    private OutboxRepository outboxRepo;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxRepo, kafkaTemplate, new EventMapper(objectMapper));
    }

    private static OutboxEvent event(String aggregateType, String aggregateId, String eventType, String payload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .processed(false)
                .build();
    }

    private static CompletableFuture<SendResult<String, String>> ok() {
        return CompletableFuture.completedFuture(null);
    }

    @Test
    @DisplayName("SELLER_APPLICATION публикуется в seller-events-topic, конверт не меняется")
    void sellerApplicationEvent_goesToSellerTopic() throws Exception {
        OutboxEvent e = event("SELLER_APPLICATION", "42", "SELLER_APPROVED", "{\"applicationId\":42}");
        when(outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e));
        when(kafkaTemplate.send(eq(SELLER_TOPIC), eq("42"), anyString())).thenReturn(ok());

        publisher.publisherEvents();

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(SELLER_TOPIC), eq("42"), value.capture());
        JsonNode envelope = objectMapper.readTree(value.getValue());
        assertThat(envelope.get("aggregateType").asText()).isEqualTo("SELLER_APPLICATION");
        assertThat(envelope.get("eventType").asText()).isEqualTo("SELLER_APPROVED");
        assertThat(envelope.get("payload").asText()).isEqualTo("{\"applicationId\":42}");
        assertThat(value.getValue()).contains("\"eventType\":\"SELLER_APPROVED\"");
        assertThat(e.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("STORE / STORE_DELETED публикуется в store-events-topic с ключом id магазина")
    void storeEvent_goesToStoreTopic() throws Exception {
        OutboxEvent e = event("STORE", "3", "STORE_DELETED", "{\"storeId\":3,\"sellerId\":7}");
        when(outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e));
        when(kafkaTemplate.send(eq(STORE_TOPIC), eq("3"), anyString())).thenReturn(ok());

        publisher.publisherEvents();

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(STORE_TOPIC), eq("3"), value.capture());
        verify(kafkaTemplate, never()).send(eq(SELLER_TOPIC), anyString(), anyString());
        JsonNode envelope = objectMapper.readTree(value.getValue());
        assertThat(envelope.get("aggregateType").asText()).isEqualTo("STORE");
        assertThat(envelope.get("eventType").asText()).isEqualTo("STORE_DELETED");
        assertThat(envelope.get("payload").asText()).isEqualTo("{\"storeId\":3,\"sellerId\":7}");
        assertThat(e.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Неизвестный aggregateType помечается processed без публикации")
    void unknownAggregateType_markedProcessedWithoutPublishing() {
        OutboxEvent e = event("ORDER", "1", "ORDER_CREATED", "{}");
        when(outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e));

        publisher.publisherEvents();

        verifyNoInteractions(kafkaTemplate);
        assertThat(e.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Неизвестный eventType у STORE помечается processed без публикации")
    void unknownStoreEventType_markedProcessedWithoutPublishing() {
        OutboxEvent e = event("STORE", "3", "STORE_RENAMED", "{}");
        when(outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(e));

        publisher.publisherEvents();

        verifyNoInteractions(kafkaTemplate);
        assertThat(e.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("Ошибка отправки прекращает цикл")
    void sendFailure_stopsBatch() {
        OutboxEvent first = event("STORE", "3", "STORE_DELETED", "{}");
        OutboxEvent second = event("SELLER_APPLICATION", "42", "SELLER_APPROVED", "{}");
        when(outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(eq(STORE_TOPIC), eq("3"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        publisher.publisherEvents();

        assertThat(first.isProcessed()).isFalse();
        assertThat(second.isProcessed()).isFalse();
        verify(kafkaTemplate, never()).send(eq(SELLER_TOPIC), any(), any());
    }
}
