package ru.klimovich.user_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import ru.klimovich.user_service.util.MessageKeys;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Переводит нарушение уникальных индексов вишлиста (гонка между проверкой в сервисе и записью) в 409.
 * Индекс определяется по имени в тексте ошибки PostgreSQL (SQLState 23505); индексы создает schema.sql
 * (change user-service-db-constraints).
 */
public final class WishlistUniqueViolationTranslator {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String WISHLIST_NAME_INDEX = "uq_wishlists_user_name";
    private static final String WISHLIST_PRODUCT_INDEX = "uq_wishlist_products";

    private WishlistUniqueViolationTranslator() {
    }

    public static Optional<ResourceConflictException> translate(DataIntegrityViolationException ex,
                                                                String wishlistName,
                                                                String productId) {
        SQLException sqlException = findUniqueViolation(ex);
        if (sqlException == null) {
            return Optional.empty();
        }
        String text = String.valueOf(sqlException.getMessage());
        if (text.contains(WISHLIST_NAME_INDEX)) {
            return Optional.of(new ResourceConflictException(
                    String.format(MessageKeys.WISHLIST_NAME_ALREADY_EXIST, wishlistName)));
        }
        if (text.contains(WISHLIST_PRODUCT_INDEX)) {
            return Optional.of(new ResourceConflictException(MessageKeys.PRODUCT_ALREADY_IN_WISHLIST, productId));
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
