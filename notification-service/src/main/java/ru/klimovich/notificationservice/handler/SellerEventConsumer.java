package ru.klimovich.notificationservice.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.klimovich.notificationservice.event.KafkaEvent;
import ru.klimovich.notificationservice.event.SellerApplicationCreatedEvent;
import ru.klimovich.notificationservice.event.SellerApprovedEvent;
import ru.klimovich.notificationservice.event.SellerRejectedEvent;
import ru.klimovich.notificationservice.mapper.EventMapper;
import ru.klimovich.notificationservice.service.seller.SellerNotificationService;

@Component
@RequiredArgsConstructor
public class SellerEventConsumer {
    private final SellerNotificationService notificationService;
    private final EventMapper eventMapper;

    @KafkaListener(
            topics = "seller-events-topic",
            groupId = "notification-group"
    )
    public void consume(String message) {
        KafkaEvent kafkaEvent = eventMapper.fromJson(message, KafkaEvent.class);


        switch (kafkaEvent.getEventType()) {
            case SELLER_APPLICATION_CREATED -> {
                SellerApplicationCreatedEvent event = eventMapper.fromJson(kafkaEvent.getPayload(), SellerApplicationCreatedEvent.class);
                notificationService.notifyModeratorAboutNewApplication(event);
            }
            case SELLER_APPROVED -> {
                SellerApprovedEvent event = eventMapper.fromJson(kafkaEvent.getPayload(), SellerApprovedEvent.class);
                notificationService.notifySellerApproved(event);
            }

            case SELLER_REJECTED -> {
                SellerRejectedEvent event = eventMapper.fromJson(kafkaEvent.getPayload(), SellerRejectedEvent.class);
                notificationService.notifySellerRejected(event);
            }
        }
    }
}
