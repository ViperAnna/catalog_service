package ru.seller_service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.event.KafkaEvent;
import ru.seller_service.event.SellerEventType;
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
    private static final String TOPIC = "seller-events-topic";

    @Scheduled(fixedRate = 3000)
    @Transactional
    public void publisherEvents() {
        List<OutboxEvent> events = outboxRepo.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
            KafkaEvent kafkaEvent = new KafkaEvent(
                    event.getAggregateType(),
                    SellerEventType.valueOf(event.getEventType()),
                    event.getPayload()
            );

            String message = eventMapper.toJson(kafkaEvent);


                kafkaTemplate
                        .send(
                                TOPIC,
                                event.getAggregateId(),
                                message
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
