package ru.klimovich.catalog_service.entity.emum;


public enum StatusCategory {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ARCHIVED("Archived"),
    DRAFT("Draft");

    StatusCategory(String description) {
    }
}
