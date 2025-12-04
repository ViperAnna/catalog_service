package ru.klimovich.catalog_service.entity.emum;

public enum StatusItem {
    OUT_OF_STOCK("Out of stock"),
    IN_STOCK("In stock"),
    PRE_ORDER("Pre order"),

    DRAFT("Draft");

    StatusItem(String description) {
    }
}
