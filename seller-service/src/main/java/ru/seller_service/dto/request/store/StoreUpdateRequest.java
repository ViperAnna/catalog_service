package ru.seller_service.dto.request.store;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Запрос изменения магазина. Поля description, email и phoneNumber
 * необязательны: null не меняет текущее значение. Если поле передано,
 * действуют те же правила формата, что при создании.
 */
public record StoreUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String storeName,

        @Size(max = 255, message = "Description can't exceed 255 characters.")
        String description,

        @Email
        String email,

        @Pattern(regexp = "^(\\+7|8)\\d{10}$",
                message = "Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX.")
        String phoneNumber) {

}
