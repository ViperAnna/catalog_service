package ru.klimovich.order_service.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.klimovich.order_service.dto.response.OrderItemResponse;
import ru.klimovich.order_service.dto.response.OrderResponse;
import ru.klimovich.order_service.dto.response.OrderSummaryResponse;
import ru.klimovich.order_service.model.Order;
import ru.klimovich.order_service.model.OrderItem;

@Mapper(componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    OrderSummaryResponse toSummary(Order order);

    OrderItemResponse toItemResponse(OrderItem item);
}
