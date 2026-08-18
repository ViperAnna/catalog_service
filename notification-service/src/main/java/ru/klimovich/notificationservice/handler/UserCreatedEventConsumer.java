package ru.klimovich.notificationservice.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.klimovich.notificationservice.service.email.EmailService;
import ru.klimovich.notificationservice.event.UserCreatedEvent;
import ru.klimovich.notificationservice.mapper.EventMapper;
import ru.klimovich.notificationservice.service.telegram.TelegramLinkService;
import ru.klimovich.notificationservice.service.telegram.TelegramService;


@Component
@Slf4j
//@KafkaListener(topics = "user-created-events-topic")
@RequiredArgsConstructor
public class UserCreatedEventConsumer {
    private final EventMapper eventMapper;
    private final EmailService emailService;
    private final TelegramService telegramService;
    private final TelegramLinkService telegramLinkService;


    @KafkaListener(
            topics = "user-created-events-topic",
            groupId = "notification-group"
    )
    public void consume(String payload) {
        log.info("CONSUME START");
        log.info("payload = {}", payload);


        UserCreatedEvent event =
                eventMapper.fromJson(payload, UserCreatedEvent.class);
        log.info("User registered: {}", event.getEmail()
        );


        String telegramLink = telegramLinkService.createTelegramLink(event);

        String text = """
                Добро пожаловать!

                Ваш аккаунт успешно зарегистрирован.
                """;
        telegramService.sendNotification(event.getUserId(), text);
        emailService.sendWelcomeEmail(event, telegramLink);

    }
}
