package ru.klimovich.order_service.domain;

/**
 * Инициатор перехода статуса: покупатель через HTTP API или система (будущие обработчики
 * событий оплаты и доставки, design.md D6). В MVP переходов SYSTEM нет.
 */
public enum OrderInitiator {
    CUSTOMER,
    SYSTEM
}
