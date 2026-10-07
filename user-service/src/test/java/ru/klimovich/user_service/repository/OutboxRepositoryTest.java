package ru.klimovich.user_service.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.klimovich.user_service.model.OutboxEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Фиксирует выборку outbox, на которую опирается OutboxPublisher
 * (requirement "USER_CREATED publishing", openspec/specs/user-profile/spec.md).
 */
class OutboxRepositoryTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private OutboxRepository outboxRepository;

    private static OutboxEvent event(String aggregateId, LocalDateTime createdAt, boolean processed) {
        return OutboxEvent.builder()
                .aggregateType("USER")
                .aggregateId(aggregateId)
                .eventType("USER_CREATED")
                .payload("{}")
                .createdAt(createdAt)
                .processed(processed)
                .build();
    }

    @Test
    @DisplayName("Выборка: только processed = false, по возрастанию createdAt")
    void findTop100_returnsOnlyUnprocessedOrderedByCreatedAt() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        outboxRepository.saveAll(List.of(
                event("late", base.plusMinutes(3), false),
                event("done", base.plusMinutes(1), true),
                event("early", base, false),
                event("middle", base.plusMinutes(2), false)
        ));
        outboxRepository.flush();

        List<OutboxEvent> result = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        assertThat(result).extracting(OutboxEvent::getAggregateId)
                .containsExactly("early", "middle", "late");
    }

    @Test
    @DisplayName("Выборка: не больше 100 записей, самые ранние")
    void findTop100_limitsTo100Earliest() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        List<OutboxEvent> events = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            events.add(event("e-" + i, base.plusSeconds(i), false));
        }
        outboxRepository.saveAll(events);
        outboxRepository.flush();

        List<OutboxEvent> result = outboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc();

        assertThat(result).hasSize(100);
        assertThat(result.get(0).getAggregateId()).isEqualTo("e-0");
        assertThat(result).extracting(OutboxEvent::getAggregateId).doesNotContain("e-100");
    }
}
