package ru.klimovich.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = true, unique = true)
    private String userId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "telegram_user_name")
    private String telegramUsername;

    @Column(name = "linked")
    private boolean linked;

    @Column(name = "link_token")
    private String linkToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
}
