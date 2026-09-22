package ru.seller_service.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.seller_service.outbox.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
