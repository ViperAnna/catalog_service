package ru.klimovich.catalog_service.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ImageBucket {
    CATEGORIES("categories"),
    PRODUCTS("products");
    private final String name;

}
