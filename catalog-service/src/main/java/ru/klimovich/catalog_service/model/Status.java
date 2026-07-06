package ru.klimovich.catalog_service.model;

import lombok.ToString;

@ToString
public enum Status {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived"),
    DRAFT("draft");

    Status(String description) {
    }
}
