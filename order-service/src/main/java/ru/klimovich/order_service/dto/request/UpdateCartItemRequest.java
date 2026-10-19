package ru.klimovich.order_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required.")
        @Min(value = 1, message = "Quantity must be between 1 and 99.")
        @Max(value = 99, message = "Quantity must be between 1 and 99.")
        Integer quantity) {
}
