package ru.klimovich.user_service.outobox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import ru.klimovich.user_service.model.OutboxEvent;
import ru.klimovich.user_service.repository.OutboxRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Фиксирует baseline-поведение requirement "USER_CREATED publishing"
 * (openspec/specs/user-profile/spec.md).
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String TOPIC = "user-created-events-topic";

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxRepository, kafkaTemplate);
    }

    private OutboxEvent event(String aggregateId, String payload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("USER")
                .aggregateId(aggregateId)
                .eventType("USER_CREATED")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .processed(false)
                .build();
    }

    private static CompletableFuture<SendResult<String, String>> ok() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<SendResult<String, String>> failed() {
        return CompletableFuture.failedFuture(new RuntimeException("broker unavailable"));
    }

    @Test
    @DisplayName("USER_CREATED publishing: ключ = aggregateId, значение = payload без обертки, processed после ack")
    void publishEvents_sendsKeyAndRawPayloadAndMarksProcessed() {
        OutboxEvent first = event("u-1", "{\"userId\":\"u-1\"}");
        OutboxEvent second = event("u-2", "{\"userId\":\"u-2\"}");
        when(outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, "u-1", "{\"userId\":\"u-1\"}")).thenReturn(ok());
        when(kafkaTemplate.send(TOPIC, "u-2", "{\"userId\":\"u-2\"}")).thenReturn(ok());

        publisher.publishEvents();

        verify(kafkaTemplate).send(TOPIC, "u-1", "{\"userId\":\"u-1\"}");
        verify(kafkaTemplate).send(TOPIC, "u-2", "{\"userId\":\"u-2\"}");
        assertThat(first.isProcessed()).isTrue();
        assertThat(second.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("USER_CREATED publishing: при ошибке отправки цикл прекращается")
    void publishEvents_stopsOnFirstFailure() {
        OutboxEvent first = event("u-1", "p1");
        OutboxEvent second = event("u-2", "p2");
        OutboxEvent third = event("u-3", "p3");
        when(outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first, second, third));
        when(kafkaTemplate.send(TOPIC, "u-1", "p1")).thenReturn(ok());
        when(kafkaTemplate.send(TOPIC, "u-2", "p2")).thenReturn(failed());

        publisher.publishEvents();

        assertThat(first.isProcessed()).isTrue();
        assertThat(second.isProcessed()).isFalse();
        assertThat(third.isProcessed()).isFalse();
        verify(kafkaTemplate, never()).send(TOPIC, "u-3", "p3");
    }

    @Test
    @DisplayName("USER_CREATED publishing: пустая выборка — отправок нет")
    void publishEvents_noEvents_noSends() {
        when(outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());

        publisher.publishEvents();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("USER_CREATED publishing: интервал fixedDelay = 3000 мс")
    void publishEvents_isScheduledWithFixedDelay3000() throws Exception {
        Scheduled scheduled = OutboxPublisher.class
                .getMethod("publishEvents")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelay()).isEqualTo(3000L);
        assertThat(scheduled.fixedRate()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("USER_CREATED publishing: используется топик user-created-events-topic")
    void publishEvents_usesUserCreatedTopic() {
        OutboxEvent first = event("u-1", "p1");
        when(outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(first));
        when(kafkaTemplate.send(TOPIC, "u-1", "p1")).thenReturn(ok());

        publisher.publishEvents();

        verify(kafkaTemplate, never()).send(org.mockito.ArgumentMatchers.argThat(t -> !TOPIC.equals(t)), anyString(), anyString());
    }
}
