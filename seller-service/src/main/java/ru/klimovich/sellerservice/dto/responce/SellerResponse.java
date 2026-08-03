package ru.klimovich.sellerservice.dto.responce;

import ru.klimovich.sellerservice.model.status.SellerStatus;

import java.time.LocalDateTime;


public record SellerResponse(
        Long id,
        String keycloakUserId,
        Integer inn,
        Integer phoneNumber,
        Integer email,
        Long paymentAccount,
        SellerStatus sellerStatus,
        LocalDateTime createdAt) {
}
