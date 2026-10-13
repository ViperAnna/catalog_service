package ru.seller_service.dto.responce;

import lombok.*;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.model.status.StoreStatus;

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
       private String phoneNumber;
       private StoreStatus status;
       private SellerStatus sellerStatus;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;
}
