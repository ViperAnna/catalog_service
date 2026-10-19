package ru.seller_service.dto.responce;

import lombok.*;
import ru.seller_service.model.status.StoreChangeRequestStatus;

import java.time.LocalDateTime;

/**
 * Заявка на изменение магазина: предлагаемые значения (null — поле не меняется) и статус.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoreChangeRequestResponse {
       private Long id;
       private Long storeId;
       private String storeName;
       private String description;
       private String email;
       private String phoneNumber;
       private StoreChangeRequestStatus status;
       private String moderatorComment;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;
}
