package ru.seller_service.dto.request.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record SellerDeletionRequest(
    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    String reason){
}
