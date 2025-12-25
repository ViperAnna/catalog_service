package ru.klimovich.catalog_service.util;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ErrorKeys {
    public static final String PRODUCT_NOT_FOUND_KEY = "error.util.ErrorKeys.product.notFound";
    public static final String PRODUCT_NOT_FOUND_NAME_KEY = "error.util.ErrorKeys.product.notFoundName";

    public static final String CATEGORY_ALREADY_EXIST = "error.util.ErrorKeys.category.alreadyExist";
    public static final String CATEGORY_NOT_FOUND_NAME_KEY = "error.util.ErrorKeys.category.notFound";
    public static final String CATEGORY_CREATED_SUCCESSFULLY = "error.util.ErrorKeys.category.successfullyCreated";
    public static final String CATEGORY_UPDATE_SUCCESSFULLY = "error.util.ErrorKeys.category.successfullyUpdated";
    public static final String CATEGORY_DELETE_SUCCESSFULLY = "error.util.ErrorKeys.category.successfullyDeleted";
    public static final String PRODUCT_CREATED_SUCCESSFULLY = "error.util.ErrorKeys.product.successfullyCreated";
    public static final String PRODUCT_UPDATE_SUCCESSFULLY = "error.util.ErrorKeys.product.successfullyUpdated";
    public static final String PRODUCT_DELETE_SUCCESSFULLY = "error.util.ErrorKeys.product.successfullyDeleted";

}
