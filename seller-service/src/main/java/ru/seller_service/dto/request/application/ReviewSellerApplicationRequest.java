package ru.seller_service.dto.request.application;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.seller_service.model.status.SellerApplicationStatus;

public record ReviewSellerApplicationRequest(

        @NotNull(message = "Status is required")
        SellerApplicationStatus status,

        @Size(max = 500)
        String moderatorComment
) {
}
