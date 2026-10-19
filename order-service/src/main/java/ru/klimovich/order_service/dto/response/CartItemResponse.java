package ru.klimovich.order_service.dto.response;

/**
 * Ответ изменяющих операций корзины: без цен, клиент перечитывает GET /cart.
 */
public record CartItemResponse(String productId, int quantity) {
}
