package ru.klimovich.order_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Позиция корзины с текущими данными каталога. У недоступной позиции заданы только
 * productId, quantity и available = false.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CartLineResponse(String productId,
                               int quantity,
                               boolean available,
                               String name,
                               Long storeId,
                               BigDecimal unitPrice,
                               BigDecimal lineTotal) {

    public static CartLineResponse unavailable(String productId, int quantity) {
        return new CartLineResponse(productId, quantity, false, null, null, null, null);
    }
}
