package ru.klimovich.notificationservice.service.telegram;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramProperties {
    @ToString.Exclude
    private String token;
    private String username;
}
