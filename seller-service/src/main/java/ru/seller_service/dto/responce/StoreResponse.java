package ru.seller_service.dto.responce;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoreResponse{
       private Long id;
       private Long sellerId;
       private String storeName;
       private String description;
       private String email;
       private String phone;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;
}
