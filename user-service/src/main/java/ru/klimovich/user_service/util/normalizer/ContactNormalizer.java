package ru.klimovich.user_service.util.normalizer;

import java.util.Locale;

/**
 * Приводит контактные данные пользователя к каноническому виду перед проверкой
 * уникальности и сохранением.
 */
public final class ContactNormalizer {

    private static final String RU_PREFIX = "+7";

    private ContactNormalizer() {
    }

    /**
     * Email в нижнем регистре без пробелов по краям.
     */
    public static String email(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Телефон РФ в формате +7XXXXXXXXXX. Ожидает значение, уже прошедшее
     * валидацию формата: +7XXXXXXXXXX, 7XXXXXXXXXX или 8XXXXXXXXXX.
     */
    public static String phone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.startsWith(RU_PREFIX)) {
            return phone;
        }
        if (phone.length() == 11 && (phone.startsWith("7") || phone.startsWith("8"))) {
            return RU_PREFIX + phone.substring(1);
        }
        return phone;
    }
}
