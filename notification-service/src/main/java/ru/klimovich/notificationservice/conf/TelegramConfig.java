package ru.klimovich.notificationservice.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.klimovich.notificationservice.telegram.TelegramProperties;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramClient telegramClient(TelegramProperties properties) {
        System.out.println("TOKEN = " + properties.getToken());
        System.out.println("USERNAME = " + properties.getUsername());
        return new OkHttpTelegramClient(properties.getToken());
    }
}
