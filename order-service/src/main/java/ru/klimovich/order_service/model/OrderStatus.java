package ru.klimovich.order_service.model;

/**
 * Статусы заказа MVP. Новые статусы добавляются вместе с CHECK в schema.sql
 * и контрактом order-events-topic (design.md D6).
 */
public enum OrderStatus {
    CREATED,
    CANCELLED
}
