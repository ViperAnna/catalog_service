package ru.klimovich.catalog_service.util;

import lombok.NoArgsConstructor;
import ru.klimovich.catalog_service.model.Product;

@NoArgsConstructor
public class ErrorKeys {
    public static final String PRODUCT_NOT_FOUND_KEY = "Product not found with id: [{0}]";
    public static final String PRODUCT_NOT_FOUND_NAME_KEY =  "No products found with name [{0}]";
    public static final String CATEGORY_ALREADY_EXIST = "Category with name [{0}] already exists";
    public static final String CATEGORY_NOT_FOUND_NAME_KEY = "Category not found with id: [{0}]";

    public static final String CATEGORY_CREATED_SUCCESSFULLY = "The category was successfully created.";
    public static final String CATEGORY_UPDATE_SUCCESSFULLY = "The category was successfully updated.";
    public static final String CATEGORY_DELETE_SUCCESSFULLY = "The category was successfully deleted.";

}
