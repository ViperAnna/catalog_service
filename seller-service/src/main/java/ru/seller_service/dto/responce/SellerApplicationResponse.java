package ru.seller_service.dto.responce;

import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerApplicationStatus;

import java.time.LocalDateTime;


public record SellerApplicationResponse(
        Long id,
        String sellerName,
        String inn,
        String phoneNumber,
        String email,
        String paymentAccount,
        SellerApplicationStatus sellerApplicationStatus,
        SellerApplicationType applicationType,

        String moderatorComment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
