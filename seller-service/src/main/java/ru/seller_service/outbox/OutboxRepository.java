package ru.seller_service.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.seller_service.outbox.OutboxEvent;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByProcessedFalse();
}
