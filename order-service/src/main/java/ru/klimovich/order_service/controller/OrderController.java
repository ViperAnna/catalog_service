package ru.klimovich.order_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.order_service.dto.response.OrderResponse;
import ru.klimovich.order_service.dto.response.OrderSummaryResponse;
import ru.klimovich.order_service.dto.response.PageResponse;
import ru.klimovich.order_service.service.OrderService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
@Tag(name = "Order Controller", description = "Заказы текущего покупателя")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Оформление заказа",
            description = "Создает заказ из всех позиций корзины по ценам каталога на момент оформления. "
                    + "Тело запроса не используется; сумму рассчитывает сервер")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Заказ создан, корзина очищена"),
            @ApiResponse(responseCode = "409", description = "Корзина пуста, есть недоступные позиции или корзина занята"),
            @ApiResponse(responseCode = "503", description = "Каталог недоступен")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> checkout() {
        OrderResponse order = orderService.checkout();
        return ResponseEntity.created(URI.create("/orders/" + order.id())).body(order);
    }

    @Operation(summary = "Получение заказа", description = "Заказ текущего пользователя с позициями")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ найден"),
            @ApiResponse(responseCode = "400", description = "Некорректный идентификатор"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @Operation(summary = "История заказов", description = "Заказы текущего пользователя постранично, без позиций")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница заказов"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры страницы или сортировки")
    })
    @GetMapping
    public PageResponse<OrderSummaryResponse> getOrders(
            @Parameter(description = "Номер страницы, от 0") @RequestParam(required = false) Integer page,
            @Parameter(description = "Размер страницы, 1-50, по умолчанию 20") @RequestParam(required = false) Integer size,
            @Parameter(description = "createdAt или totalAmount с направлением asc|desc, по умолчанию createdAt,desc")
            @RequestParam(required = false) String sort) {
        return orderService.getOrders(page, size, sort);
    }

    @Operation(summary = "Отмена заказа", description = "Переводит заказ из CREATED в CANCELLED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Заказ отменен"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден"),
            @ApiResponse(responseCode = "409", description = "Отмена недопустима в текущем статусе или заказ изменен параллельно")
    })
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable UUID orderId) {
        return orderService.cancel(orderId);
    }
}
