package ru.klimovich.user_service.exception;

/**
 * Email для аккаунта продавца уже принадлежит другому аккаунту Keycloak.
 * В тексте нет адреса: исключение попадает в лог.
 */
public class SellerAccountConflictException extends RuntimeException {

    public SellerAccountConflictException() {
        super("Email is already used by another account");
    }
}
