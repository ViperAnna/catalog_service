package ru.seller_service.util;

/**
 * Приводит телефон магазина к формату +7XXXXXXXXXX. Ожидает значение, уже
 * прошедшее валидацию формата (+7XXXXXXXXXX или 8XXXXXXXXXX).
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() == 11 && phone.startsWith("8")) {
            return "+7" + phone.substring(1);
        }
        return phone;
    }
}
