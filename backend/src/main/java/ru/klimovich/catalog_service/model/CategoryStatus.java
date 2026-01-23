package ru.klimovich.catalog_service.model;

public enum CategoryStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived"),
    DRAFT("draft");

    CategoryStatus(String description) {
    }
}
