package ru.klimovich.order_service.domain;

import java.math.BigDecimal;

/**
 * Товар, прошедший CatalogItemPolicy: цена приведена к 2 знакам, магазин и название заданы.
 */
public record AvailableProduct(String productId,
                               Long storeId,
                               String name,
                               String articleNumber,
                               BigDecimal unitPrice) {
}
