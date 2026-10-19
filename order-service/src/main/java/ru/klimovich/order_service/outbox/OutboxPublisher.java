package ru.klimovich.order_service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.order_service.domain.OrderEventFactory;
import ru.klimovich.order_service.mapper.EventMapper;
import ru.klimovich.order_service.model.OrderStatus;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Публикация outbox в order-events-topic по образцу seller-service (integration-events "Outbox publishing").
 * Строки outbox не блокируются, поэтому сервис работает в одном экземпляре (design.md D11).
 * В лог пишутся только id записи (eventId) и eventType, payload и email — никогда.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    static final String ORDER_TOPIC = "order-events-topic";

    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventMapper eventMapper;

    @Scheduled(fixedRate = 3000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                validateType(event);

                KafkaEvent kafkaEvent = new KafkaEvent(
                        event.getAggregateType(),
                        event.getEventType(),
                        event.getPayload()
                );

                kafkaTemplate
                        .send(
                                ORDER_TOPIC,
                                event.getAggregateId(),
                                eventMapper.toJson(kafkaEvent)
                        )
                        .get(10, TimeUnit.SECONDS);

                markProcessed(event);

            } catch (IllegalArgumentException e) {
                log.error("Unknown aggregate type {} or event type {} in outbox event {}, marked processed without publishing",
                        event.getAggregateType(), event.getEventType(), event.getId());
                markProcessed(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Publishing interrupted for outbox event {}", event.getId());
                return;
            } catch (ExecutionException | TimeoutException e) {
                log.error("Failed to publish outbox event {} ({}): {}",
                        event.getId(), event.getEventType(), e.getClass().getSimpleName());
                return;
            }
        }
    }

    /**
     * Вызывается только после подтверждения Kafka (или когда публикация не требуется).
     * Обезличивание в той же транзакции: если commit не пройдет, запись останется
     * processed = false с исходным payload и будет отправлена снова с тем же eventId.
     */
    private void markProcessed(OutboxEvent event) {
        if (OrderEventFactory.AGGREGATE_TYPE.equals(event.getAggregateType())) {
            event.setPayload(eventMapper.toJson(redacted(event)));
        }
        event.setProcessed(true);
    }

    private static Object redacted(OutboxEvent event) {
        return OrderEventFactory.redactedPayload(event.getAggregateId(), event.getId());
    }

    /**
     * Известны только события ORDER с eventType ORDER_<OrderStatus>. Иначе IllegalArgumentException:
     * запись помечается processed без публикации.
     */
    private static void validateType(OutboxEvent event) {
        if (!OrderEventFactory.AGGREGATE_TYPE.equals(event.getAggregateType())) {
            throw new IllegalArgumentException("Unknown aggregate type " + event.getAggregateType());
        }
        for (OrderStatus status : OrderStatus.values()) {
            if (OrderEventFactory.eventType(status).equals(event.getEventType())) {
                return;
            }
        }
        throw new IllegalArgumentException("Unknown event type " + event.getEventType());
    }
}
