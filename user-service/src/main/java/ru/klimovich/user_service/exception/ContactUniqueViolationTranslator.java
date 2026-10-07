package ru.klimovich.user_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import ru.klimovich.user_service.util.MessageKeys;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Переводит нарушение unique constraint email или phone (гонка между проверкой
 * в сервисе и записью) в 409. Поле определяется по ошибке PostgreSQL
 * (SQLState 23505 и "Key (column)=" в detail), без привязки к именам constraint.
 */
public final class ContactUniqueViolationTranslator {

    private static final String UNIQUE_VIOLATION = "23505";

    private ContactUniqueViolationTranslator() {
    }

    public static Optional<ResourceConflictException> translate(DataIntegrityViolationException ex,
                                                                String email,
                                                                String phone) {
        SQLException sqlException = findUniqueViolation(ex);
        if (sqlException == null) {
            return Optional.empty();
        }
        String text = String.valueOf(sqlException.getMessage());
        if (text.contains("(email)=")) {
            return Optional.of(new ResourceConflictException(MessageKeys.USER_EMAIL_ALREADY_EXIST, email));
        }
        if (text.contains("(phone)=")) {
            return Optional.of(new ResourceConflictException(MessageKeys.USER_PHONE_ALREADY_EXIST, phone));
        }
        return Optional.empty();
    }

    private static SQLException findUniqueViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return sql;
            }
            current = current.getCause();
        }
        return null;
    }
}
