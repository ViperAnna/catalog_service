package ru.klimovich.order_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.order_service.domain.AvailableProduct;
import ru.klimovich.order_service.domain.CartPricing;
import ru.klimovich.order_service.domain.OrderInitiator;
import ru.klimovich.order_service.dto.response.OrderResponse;
import ru.klimovich.order_service.dto.response.OrderSummaryResponse;
import ru.klimovich.order_service.dto.response.PageResponse;
import ru.klimovich.order_service.exception.BadRequestException;
import ru.klimovich.order_service.exception.ResourceConflictException;
import ru.klimovich.order_service.exception.ResourceNotFoundException;
import ru.klimovich.order_service.mapper.OrderMapper;
import ru.klimovich.order_service.model.Cart;
import ru.klimovich.order_service.model.CartItem;
import ru.klimovich.order_service.model.Order;
import ru.klimovich.order_service.model.OrderItem;
import ru.klimovich.order_service.model.OrderStatus;
import ru.klimovich.order_service.repository.CartItemRepository;
import ru.klimovich.order_service.repository.OrderRepository;
import ru.klimovich.order_service.security.CurrentUserService;
import ru.klimovich.order_service.service.OrderService;
import ru.klimovich.order_service.util.MessageKeys;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Заказы текущего пользователя (spec orders). Все выборки — по паре (id, sub);
 * чтение заказа не обращается к каталогу (design.md D7).
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "totalAmount");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final OrderRepository orderRepo;
    private final CartItemRepository cartItemRepo;
    private final CartLocker cartLocker;
    private final CatalogAvailability catalogAvailability;
    private final OrderLifecycle orderLifecycle;
    private final OrderMapper orderMapper;
    private final CurrentUserService currentUserService;

    /**
     * Оформление в одной транзакции (design.md D5): блокировка корзины сериализует параллельные
     * оформления; заказ, позиции, событие и очистка корзины фиксируются вместе. Вызов каталога
     * транзакцией не является: при его ошибке ничего не изменено.
     * Заголовок Idempotency-Key не обрабатывается (контракт D12, change order-checkout-idempotency):
     * проверка ключа будет выполняться здесь, сразу после блокировки корзины.
     */
    @Override
    @Transactional
    public OrderResponse checkout() {
        String userId = currentUserService.getUserId();
        Cart cart = cartLocker.lock(userId)
                .orElseThrow(() -> new ResourceConflictException(MessageKeys.CART_IS_EMPTY));
        List<CartItem> cartItems = cartItemRepo.findByCartOrderByCreatedAtAscIdAsc(cart);
        if (cartItems.isEmpty()) {
            throw new ResourceConflictException(MessageKeys.CART_IS_EMPTY);
        }

        Map<String, AvailableProduct> available = catalogAvailability.availableProducts(
                cartItems.stream().map(CartItem::getProductId).toList(),
                currentUserService.getAccessToken());
        List<String> unavailable = cartItems.stream()
                .map(CartItem::getProductId)
                .filter(id -> !available.containsKey(id))
                .toList();
        if (!unavailable.isEmpty()) {
            throw new ResourceConflictException(MessageKeys.CART_ITEMS_UNAVAILABLE, String.join(", ", unavailable));
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .itemsCount(cartItems.size())
                .build();
        List<BigDecimal> lineTotals = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem item = snapshot(order, available.get(cartItem.getProductId()), cartItem.getQuantity());
            order.getItems().add(item);
            lineTotals.add(item.getLineTotal());
        }
        order.setTotalAmount(CartPricing.total(lineTotals));

        Order saved = orderRepo.saveAndFlush(order);
        orderLifecycle.created(saved, currentUserService.getVerifiedEmail());
        cartItemRepo.deleteAllByCart(cart);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return orderMapper.toResponse(ownOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOrders(Integer page, Integer size, String sort) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : DEFAULT_PAGE_SIZE;
        if (pageNumber < 0) {
            throw new BadRequestException(MessageKeys.INVALID_PAGE);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BadRequestException(MessageKeys.INVALID_SIZE, MAX_PAGE_SIZE);
        }
        PageRequest pageable = PageRequest.of(pageNumber, pageSize, parseSort(sort));
        return PageResponse.of(orderRepo.findByUserId(currentUserService.getUserId(), pageable)
                .map(orderMapper::toSummary));
    }

    /**
     * Отмена покупателем: переход проверяет OrderLifecycle. Параллельная отмена того же заказа
     * завершается optimistic lock (version) — 409, событие одно.
     */
    @Override
    @Transactional
    public OrderResponse cancel(UUID orderId) {
        Order order = ownOrder(orderId);
        orderLifecycle.transition(order, OrderStatus.CANCELLED, OrderInitiator.CUSTOMER,
                currentUserService.getVerifiedEmail());
        Order saved = orderRepo.saveAndFlush(order);
        return orderMapper.toResponse(saved);
    }

    private Order ownOrder(UUID orderId) {
        return orderRepo.findByIdAndUserId(orderId, currentUserService.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.ORDER_NOT_FOUND, orderId));
    }

    private static OrderItem snapshot(Order order, AvailableProduct product, int quantity) {
        String name = product.name();
        if (name.length() > OrderItem.PRODUCT_NAME_MAX_LENGTH) {
            name = name.substring(0, OrderItem.PRODUCT_NAME_MAX_LENGTH);
        }
        return OrderItem.builder()
                .order(order)
                .productId(product.productId())
                .storeId(product.storeId())
                .productName(name)
                .articleNumber(product.articleNumber())
                .unitPrice(product.unitPrice())
                .quantity(quantity)
                .lineTotal(CartPricing.lineTotal(product.unitPrice(), quantity))
                .build();
    }

    /**
     * sort: createdAt или totalAmount, направление asc|desc (по умолчанию asc, как в Spring Data);
     * без параметра — createdAt,desc.
     */
    static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }
        String[] parts = sort.split(",", -1);
        if (parts.length > 2 || !SORT_FIELDS.contains(parts[0].trim())) {
            throw new BadRequestException(MessageKeys.INVALID_SORT);
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length == 2) {
            String dir = parts[1].trim().toLowerCase();
            if (!dir.equals("asc") && !dir.equals("desc")) {
                throw new BadRequestException(MessageKeys.INVALID_SORT);
            }
            direction = dir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        }
        return Sort.by(direction, parts[0].trim());
    }
}
