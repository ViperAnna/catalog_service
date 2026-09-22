package ru.klimovich.user_service.outobox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.model.OutboxEvent;
import ru.klimovich.user_service.repository.OutboxRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private static final String TOPIC = "user-created-events-topic";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                                TOPIC,
                                event.getAggregateId(),
                                event.getPayload()
                        )
                        .get(10, TimeUnit.SECONDS);
                event.setProcessed(true);
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
}
