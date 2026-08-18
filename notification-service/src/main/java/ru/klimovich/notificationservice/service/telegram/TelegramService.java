package ru.klimovich.notificationservice.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.klimovich.notificationservice.model.TelegramUser;
import ru.klimovich.notificationservice.repository.TelegramUserRepository;

@Service
@RequiredArgsConstructor
public class TelegramService {
    private final NotificationBot bot;
    private final TelegramUserRepository repository;

    public void sendNotification(String userId, String text) {

        repository.findByUserId(userId)
                .filter(TelegramUser::isLinked)
                .ifPresent(user ->
                        bot.sendMessage(
                                user.getChatId(),
                                text));

    }
}
