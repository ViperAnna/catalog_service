package ru.klimovich.catalog_service.model;

public enum ProductStatus {
    OUT_OF_STOCK("Out of stock"),
    IN_STOCK("In stock"),
    PRE_ORDER("Pre order"),
    DRAFT("Draft");

    ProductStatus(String description) {
    }
}
