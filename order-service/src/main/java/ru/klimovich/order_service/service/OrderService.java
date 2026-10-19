package ru.klimovich.order_service.service;

import ru.klimovich.order_service.dto.response.OrderResponse;
import ru.klimovich.order_service.dto.response.OrderSummaryResponse;
import ru.klimovich.order_service.dto.response.PageResponse;

import java.util.UUID;

public interface OrderService {

    OrderResponse checkout();

    OrderResponse getOrder(UUID orderId);

    PageResponse<OrderSummaryResponse> getOrders(Integer page, Integer size, String sort);

    OrderResponse cancel(UUID orderId);
}
