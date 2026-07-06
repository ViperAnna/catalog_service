package ru.klimovich.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.notificationservice.model.TelegramUser;

import java.util.Optional;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {

    Optional<TelegramUser> findByUserId(String userId);

    Optional<TelegramUser> findByChatId(Long userId);

    Optional<TelegramUser> findByLinkToken(String token);
//    Optional<TelegramUser> findByTokenAndUsedFalse(String token);
}
