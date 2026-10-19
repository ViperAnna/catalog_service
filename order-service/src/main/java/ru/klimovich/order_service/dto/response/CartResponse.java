package ru.klimovich.order_service.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * total — сумма lineTotal доступных позиций, itemsCount — число позиций корзины.
 */
public record CartResponse(List<CartLineResponse> items, BigDecimal total, int itemsCount) {
}
