package ru.seller_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.seller_service.event.StoreEvent;
import ru.seller_service.event.StoreEventType;
import ru.seller_service.mapper.EventMapper;

import java.time.LocalDateTime;

/**
 * Записывает событие магазина в outbox в текущей транзакции.
 * Ключ сообщения — storeId, поэтому все события одного магазина публикуются по порядку.
 */
@Component
@RequiredArgsConstructor
public class StoreEventWriter {

    private static final String STORE = "STORE";

    private final OutboxRepository outboxRepo;
    private final EventMapper eventMapper;

    public void write(Long storeId, Long sellerId, StoreEventType eventType) {
        StoreEvent event = StoreEvent.builder()
                .storeId(storeId)
                .sellerId(sellerId)
                .build();
        outboxRepo.save(
                OutboxEvent.builder()
                        .aggregateType(STORE)
                        .aggregateId(storeId.toString())
                        .eventType(eventType.name())
                        .payload(eventMapper.toJson(event))
                        .createdAt(LocalDateTime.now())
                        .processed(false)
                        .build()
        );
    }
}
