package ru.seller_service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.event.KafkaEvent;
import ru.seller_service.event.SellerEventType;
import ru.seller_service.event.StoreEventType;
import ru.seller_service.mapper.EventMapper;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventMapper eventMapper;
    private static final String SELLER_TOPIC = "seller-events-topic";
    private static final String STORE_TOPIC = "store-events-topic";
    private static final String SELLER_APPLICATION = "SELLER_APPLICATION";
    private static final String STORE = "STORE";

    @Scheduled(fixedRate = 3000)
    @Transactional
    public void publisherEvents() {
        List<OutboxEvent> events = outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                String topic = resolveTopic(event);

                KafkaEvent kafkaEvent = new KafkaEvent(
                        event.getAggregateType(),
                        event.getEventType(),
                        event.getPayload()
                );

                String message = eventMapper.toJson(kafkaEvent);

                kafkaTemplate
                        .send(
                                topic,
                                event.getAggregateId(),
                                message
                        )
                        .get(10, TimeUnit.SECONDS);

                markProcessed(event);

            } catch (IllegalArgumentException e) {
                log.error(
                        "Unknown event type {} in outbox event {}",
                        event.getEventType(),
                        event.getId(),
                        e
                );
                markProcessed(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error(
                        "Publishing interrupted for outbox event {}",
                        event.getId(),
                        e
                );
                return;

            } catch (ExecutionException | TimeoutException e) {
                log.error(
                        "Failed to publish outbox event {}",
                        event.getId(),
                        e
                );
                return;
            }

        }
    }

    /**
     * Вызывается только после подтверждения Kafka (или когда публикация не требуется).
     * Обезличивание в той же транзакции: если commit не пройдет, запись останется
     * processed = false с исходным payload и будет отправлена снова.
     */
    private void markProcessed(OutboxEvent event) {
        if (OutboxRedaction.containsPersonalData(event)) {
            OutboxRedaction.redact(event, eventMapper);
        }
        event.setProcessed(true);
    }

    /**
     * Топик по aggregateType. Неизвестный aggregateType или eventType дает
     * IllegalArgumentException: запись помечается processed без публикации.
     */
    private static String resolveTopic(OutboxEvent event) {
        String aggregateType = event.getAggregateType();
        if (SELLER_APPLICATION.equals(aggregateType)) {
            SellerEventType.valueOf(event.getEventType());
            return SELLER_TOPIC;
        }
        if (STORE.equals(aggregateType)) {
            StoreEventType.valueOf(event.getEventType());
            return STORE_TOPIC;
        }
        throw new IllegalArgumentException("Unknown aggregate type " + aggregateType);
    }
}
