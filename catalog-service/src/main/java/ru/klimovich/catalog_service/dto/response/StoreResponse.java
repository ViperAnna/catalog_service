package ru.klimovich.catalog_service.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class StoreResponse {
       private Long id;
       private Long sellerId;
       private String storeName;
       private String description;
       private String email;
       private String phoneNumber;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;
}
