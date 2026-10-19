package ru.klimovich.catalog_service.outbox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Событие PRODUCT_DELETED в outbox (change product-deletion-sync, D2). id = "PRODUCT_DELETED:" + productId,
 * поэтому повторное удаление того же товара не создает второй документ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_events")
public class ProductOutboxEvent {

    @Id
    private String id;

    private String eventId;

    private String productId;

    private Long storeId;

    private ProductOutboxStatus status;

    private Instant createdAt;
}
