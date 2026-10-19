package ru.seller_service.dto.request.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тело reject. Статус заявки выставляет сервер (REJECTED), поэтому поля status в теле нет.
 */
public record ReviewSellerApplicationRequest(

        @NotBlank(message = "Rejection reason is required.")
        @Size(max = 500)
        String moderatorComment
) {
}
