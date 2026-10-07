package ru.seller_service.event;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDeletedEvent {
        Long storeId;
        Long sellerId;
}
