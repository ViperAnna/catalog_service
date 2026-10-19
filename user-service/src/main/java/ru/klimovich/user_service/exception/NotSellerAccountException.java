package ru.klimovich.user_service.exception;

/**
 * Попытка удалить через DeleteSellerAccount аккаунт без атрибута продавца (например, аккаунт покупателя).
 */
public class NotSellerAccountException extends RuntimeException {

    public NotSellerAccountException(String keycloakUserId) {
        super("Account [%s] is not a seller account".formatted(keycloakUserId));
    }
}
