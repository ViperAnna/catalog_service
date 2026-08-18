package ru.klimovich.notificationservice.service.telegram;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.klimovich.notificationservice.repository.TelegramUserRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final TelegramProperties properties;
    private final TelegramClient telegramClient;
    private final TelegramUserRepository repository;

    @PostConstruct
    public void init() {
        System.out.println("TOKEN = " + properties.getToken());
        System.out.println("BOT = " + properties.getUsername());
    }

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @SneakyThrows
    @Override
    public void consume(Update update) {
        System.out.println("UPDATE RECEIVED");

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;

        }
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getFirstName();


        log.info("Text: {}", text);
        log.info("ChatId: {}", chatId);

        String welcomeText = """
                🎉Telegram connected successfully!
                               
                Now you will receive notifications here.
                """;

        log.info("Received message");
        log.info("ChatId: {}", chatId);
        log.info("Username: {}", username);
        log.info("Text: {}", text);

        if (text.startsWith("/start")) {

            log.info("/start command received");

            String[] parts = text.split(" ");

            log.info("Parts count: {}", parts.length);

            if (parts.length != 2) {
                log.warn("Token is missing");
                sendMessage(chatId, "Usage: /start <token>");
                return;
            }

            String token = parts[1];
            log.info("Token: {}", token);

            var optionalUser = repository.findByLinkToken(token);

            log.info("User found: {}", optionalUser.isPresent());

            if (optionalUser.isEmpty()) {
                log.warn("No user with token {}", token);
                sendMessage(chatId, "Invalid token.");
                return;
            }

            var user = optionalUser.get();

            log.info("Database user:");
            log.info("id={}", user.getId());
            log.info("userId={}", user.getUserId());
            log.info("linked={}", user.isLinked());
            log.info("expiresAt={}", user.getTokenExpiresAt());

            if (user.isLinked()) {
                log.warn("User already linked");
                sendMessage(chatId, "Telegram is already connected.");
                return;
            }

            if (user.getTokenExpiresAt() == null) {
                log.warn("Token expiration is null");
                sendMessage(chatId, "Token is invalid.");
                return;
            }

            log.info("Now={}", LocalDateTime.now());

            if (user.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Token expired");
                sendMessage(chatId, "Link has expired.");
                return;
            }

            log.info("Updating user...");

            user.setChatId(chatId);
            user.setTelegramUsername(username);
            user.setLinked(true);
            user.setLinkToken(null);
            user.setTokenExpiresAt(null);

            repository.save(user);

            log.info("User saved successfully");

            sendMessage(chatId, welcomeText);

            log.info("Welcome message sent");

            return;
        }
    }

    void sendMessage(Long chatId, String text) {
        log.info("Sending message to {}: {}", chatId, text);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
            log.info("Message sent successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message", e);
        }
    }
}


