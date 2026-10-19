package ru.klimovich.user_service.service;

/**
 * Отдельные аккаунты продавцов в Keycloak (change separate-seller-account).
 */
public interface SellerAccountService {

    /**
     * Создает аккаунт продавца только с ролью SELLER и отправляет письмо установки пароля.
     *
     * @return id аккаунта в Keycloak (существующего незавершенного аккаунта продавца с тем же email при повторе)
     * @throws ru.klimovich.user_service.exception.SellerAccountConflictException email занят другим аккаунтом
     */
    String createSellerAccount(String email, String sellerName);

    /**
     * Удаляет аккаунт продавца; отсутствующий аккаунт — не ошибка.
     *
     * @throws ru.klimovich.user_service.exception.NotSellerAccountException аккаунт не является аккаунтом продавца
     */
    void deleteSellerAccount(String keycloakUserId);

    boolean isEmailAvailable(String email);
}
