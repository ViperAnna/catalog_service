package ru.klimovich.notificationservice.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.klimovich.notificationservice.service.telegram.TelegramProperties;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(TelegramProperties properties) {
        return new OkHttpTelegramClient(properties.getToken());
    }
}
