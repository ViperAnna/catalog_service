package ru.klimovich.order_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Цена в запросе не принимается: неизвестные поля (например, price) игнорируются.
 */
public record AddCartItemRequest(
        @NotBlank(message = "Product id is required.")
        @Size(max = 64, message = "Product id can't exceed 64 characters.")
        String productId,

        @NotNull(message = "Quantity is required.")
        @Min(value = 1, message = "Quantity must be between 1 and 99.")
        @Max(value = 99, message = "Quantity must be between 1 and 99.")
        Integer quantity) {
}
