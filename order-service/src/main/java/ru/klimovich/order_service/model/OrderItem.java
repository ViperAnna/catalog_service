package ru.klimovich.order_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Снимок позиции на момент оформления (design.md D7). Не связан с каталогом:
 * изменение или удаление товара и магазина позицию не меняет.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    public static final int PRODUCT_NAME_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false, length = 64)
    private String productId;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(name = "product_name", nullable = false, updatable = false, length = PRODUCT_NAME_MAX_LENGTH)
    private String productName;

    @Column(name = "article_number", updatable = false)
    private String articleNumber;

    @Column(name = "unit_price", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
