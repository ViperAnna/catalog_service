package ru.klimovich.notificationservice.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramProperties {
    private String token;
    private String username;
}
