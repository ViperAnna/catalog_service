package ru.klimovich.catalog_service.model;

public enum CategoryStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ARCHIVED("Archived"),
    DRAFT("Draft");

    CategoryStatus(String description) {
    }
}
