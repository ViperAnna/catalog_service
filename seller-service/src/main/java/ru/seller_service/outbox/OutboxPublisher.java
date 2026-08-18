package ru.seller_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 3000)
    public void publisherEvents(){
        List<OutboxEvent> events = outboxRepo.findByProcessedFalse();

        for(OutboxEvent event: events){
            kafkaTemplate.send(
                    "seller-events-topic",
                    event.getAggregateId(),
                    event.getPayload()
            );
            event.setProcessed(true);
            outboxRepo.save(event);
        }

    }
}
