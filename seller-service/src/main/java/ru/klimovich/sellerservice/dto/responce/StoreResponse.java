package ru.klimovich.sellerservice.dto.responce;

import java.time.LocalDateTime;


public record StoreResponse(
        Long id,
        Long sellerId,
        String storeName,
        String description,
        String email,
        String phone,
        boolean verified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
