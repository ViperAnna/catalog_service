package ru.seller_service.event;


import lombok.*;

/**
 * Payload всех событий store-events-topic: только идентификаторы, без персональных данных.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreEvent {
        Long storeId;
        Long sellerId;
}
