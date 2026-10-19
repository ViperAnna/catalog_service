package ru.klimovich.order_service.util;

public class MessageKeys {
    public static final String PRODUCT_NOT_AVAILABLE = "Product [%s] is not available.";
    public static final String CART_ITEM_ALREADY_EXISTS = "Product [%s] is already in the cart. Use PUT /cart/items/%s to change quantity.";
    public static final String CART_ITEM_NOT_FOUND = "Product [%s] is not in the cart.";
    public static final String CART_ITEMS_LIMIT_EXCEEDED = "Cart can contain at most %d items.";
    public static final String CART_IS_EMPTY = "Cart is empty.";
    public static final String CART_IS_BUSY = "Cart is being modified by another request. Try again.";
    public static final String CART_ITEMS_UNAVAILABLE = "Some cart items are not available: %s.";

    public static final String ORDER_NOT_FOUND = "Order not found with id: [%s].";
    public static final String ORDER_TRANSITION_NOT_ALLOWED = "Order [%s] cannot change status from %s to %s.";
    public static final String ORDER_CONCURRENT_MODIFICATION = "Order [%s] was modified by another request. Try again.";

    public static final String INVALID_PAGE = "Parameter page must be greater than or equal to 0.";
    public static final String INVALID_SIZE = "Parameter size must be between 1 and %d.";
    public static final String INVALID_SORT = "Parameter sort must be one of: createdAt, totalAmount with direction asc or desc.";

    public static final String CATALOG_SERVICE_UNAVAILABLE = "Catalog service is unavailable.";
}
