package ru.klimovich.notificationservice.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.klimovich.notificationservice.event.UserCreatedEvent;
import ru.klimovich.notificationservice.model.TelegramUser;
import ru.klimovich.notificationservice.repository.TelegramUserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramLinkService {
    private final TelegramUserRepository repository;

    public String createTelegramLink(UserCreatedEvent event) {
        log.info("========== createTelegramLink ==========");
        log.info("event.userId = '{}'", event.getUserId());

        Optional<TelegramUser> existing = repository.findByUserId(event.getUserId());

        log.info("existing.isPresent() = {}", existing.isPresent());

        repository.findAll().forEach(u ->
                log.info("DB -> id={}, userId='{}'",
                        u.getId(),
                        u.getUserId()));

        if (existing.isPresent()) {

            TelegramUser user = existing.get();

            if (user.isLinked()) {
                log.info("Telegram already linked for {}", event.getUserId());
                return null;
            }

            if (user.getLinkToken() != null) {
                return "https://t.me/myNotificServiceJavaAppBot?start=" + user.getLinkToken();
            }

            // токен потеряли — обновляем существующую запись
            String token = UUID.randomUUID().toString();

            user.setLinkToken(token);
            user.setTokenExpiresAt(LocalDateTime.now().plusDays(1));

            repository.save(user);

            return "https://t.me/myNotificServiceJavaAppBot?start=" + token;
        }

        String token = UUID.randomUUID().toString();

        TelegramUser entity = TelegramUser.builder()
                .userId(event.getUserId())
                .linkToken(token)
                .linked(false)
                .createdAt(LocalDateTime.now())
                .tokenExpiresAt(LocalDateTime.now().plusDays(1))
                .build();

        repository.save(entity);

        return "https://t.me/myNotificServiceJavaAppBot?start=" + token;
    }
}
