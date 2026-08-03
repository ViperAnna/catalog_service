package ru.klimovich.sellerservice.dto.responce;

import ru.klimovich.sellerservice.model.status.SellerApplicationStatus;

import java.time.LocalDateTime;


public record SellerApplicationResponse(
        Long id,
        String keycloakUserId,
        String inn,
        String phone,
        String email,
        String paymentAccount,
        SellerApplicationStatus sellerApplicationStatus,
        String moderatorComment,
        LocalDateTime createdAt) {
}
