package ru.klimovich.user_service.outobox;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.klimovich.user_service.model.OutboxEvent;
import ru.klimovich.user_service.repository.OutboxRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 3000)
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();

        for (OutboxEvent event : events) {
            kafkaTemplate.send(
                    "user-created-events-topic",
                    event.getAggregateId(),
                    event.getPayload()
            );
            event.setProcessed(true);
            outboxRepository.save(event);
        }
    }
}
