package ru.klimovich.user_service.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WishlistResponse {
    Long id;
    String name;
    Set<ProductResponse> products;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
