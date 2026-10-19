package ru.klimovich.notificationservice.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import ru.klimovich.notificationservice.event.OrderEvent;
import ru.klimovich.notificationservice.model.ProcessedOrderEvent;
import ru.klimovich.notificationservice.repository.ProcessedOrderEventRepository;
import ru.klimovich.notificationservice.service.email.EmailService;
import ru.klimovich.notificationservice.service.order.OrderEventClassifier.OrderEventClass;

import java.time.LocalDateTime;

/**
 * Идемпотентная обработка корректного события заказа по eventId (design.md D10 change add-order-service).
 * eventId отмечается обработанным только после успешной отправки письма или осознанного пропуска.
 * В лог пишутся только eventId, orderId и eventType — не email и не payload.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {

    private final EmailService emailService;
    private final ProcessedOrderEventRepository processedRepo;

    public void process(OrderEvent event, String eventType, OrderEventClass eventClass) {
        if (processedRepo.existsById(event.getEventId())) {
            log.info("Duplicate order event {} (order {}, {}), skipped", event.getEventId(), event.getOrderId(), eventType);
            return;
        }

        switch (eventClass) {
            case SKIPPED_BY_POLICY -> log.info("Order event {} (order {}, {}) skipped by policy: no notification",
                    event.getEventId(), event.getOrderId(), eventType);
            case ORDER_CREATED, ORDER_CANCELLED -> {
                if (event.getEmail() == null || event.getEmail().isBlank()) {
                    log.warn("Order event {} (order {}, {}) has no recipient, email is not sent",
                            event.getEventId(), event.getOrderId(), eventType);
                } else {
                    send(event, eventType, eventClass);
                    log.info("Order event {} (order {}, {}) notification sent", event.getEventId(), event.getOrderId(), eventType);
                }
            }
            case UNKNOWN -> throw new IllegalStateException("Unknown order events are not processed");
        }

        markProcessed(event, eventType);
    }

    /**
     * Ошибка SMTP — временная: пробрасывается для повторов и DLT, eventId не отмечается.
     */
    private void send(OrderEvent event, String eventType, OrderEventClass eventClass) {
        try {
            if (eventClass == OrderEventClass.ORDER_CREATED) {
                emailService.sendOrderCreatedEmail(event);
            } else {
                emailService.sendOrderCancelledEmail(event);
            }
        } catch (MailException e) {
            log.error("Unable to send email for order event {} (order {}, {}): {}",
                    event.getEventId(), event.getOrderId(), eventType, e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * Параллельная обработка того же eventId (нарушение PK) — дубликат, не ошибка.
     */
    private void markProcessed(OrderEvent event, String eventType) {
        try {
            processedRepo.saveAndFlush(ProcessedOrderEvent.builder()
                    .eventId(event.getEventId())
                    .orderId(event.getOrderId())
                    .eventType(eventType)
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("Order event {} (order {}, {}) already marked processed", event.getEventId(), event.getOrderId(), eventType);
        }
    }
}
