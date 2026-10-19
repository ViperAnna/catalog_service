package ru.klimovich.order_service.dto.response;

import java.math.BigDecimal;

/**
 * Снимок позиции заказа на момент оформления.
 */
public record OrderItemResponse(String productId,
                                Long storeId,
                                String productName,
                                String articleNumber,
                                BigDecimal unitPrice,
                                int quantity,
                                BigDecimal lineTotal) {
}
