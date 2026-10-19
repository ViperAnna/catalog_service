package ru.klimovich.order_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.order_service.dto.request.AddCartItemRequest;
import ru.klimovich.order_service.dto.request.UpdateCartItemRequest;
import ru.klimovich.order_service.dto.response.CartItemResponse;
import ru.klimovich.order_service.dto.response.CartResponse;
import ru.klimovich.order_service.service.CartService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
@Tag(name = "Cart Controller", description = "Корзина текущего покупателя")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Получение корзины",
            description = "Возвращает позиции корзины с текущими ценами каталога и сумму доступных позиций")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Корзина возвращена"),
            @ApiResponse(responseCode = "503", description = "Каталог недоступен")
    })
    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @Operation(summary = "Добавление товара в корзину",
            description = "Добавляет доступный товар; цена из запроса не принимается")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Позиция добавлена"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "404", description = "Товар недоступен или не существует"),
            @ApiResponse(responseCode = "409", description = "Товар уже в корзине, лимит позиций или корзина занята"),
            @ApiResponse(responseCode = "503", description = "Каталог недоступен")
    })
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request);
    }

    @Operation(summary = "Изменение количества",
            description = "Устанавливает количество позиции без обращения к каталогу")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Количество изменено"),
            @ApiResponse(responseCode = "400", description = "Некорректное количество"),
            @ApiResponse(responseCode = "404", description = "Товара нет в корзине"),
            @ApiResponse(responseCode = "409", description = "Корзина занята")
    })
    @PutMapping("/items/{productId}")
    public CartItemResponse updateQuantity(@PathVariable String productId,
                                           @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateQuantity(productId, request);
    }

    @Operation(summary = "Удаление позиции")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Позиция удалена"),
            @ApiResponse(responseCode = "404", description = "Товара нет в корзине")
    })
    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable String productId) {
        cartService.removeItem(productId);
    }

    @Operation(summary = "Очистка корзины", description = "Удаляет все позиции; заказы не затрагиваются")
    @ApiResponse(responseCode = "204", description = "Корзина очищена")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        cartService.clear();
    }
}
