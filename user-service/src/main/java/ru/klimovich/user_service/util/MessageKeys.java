package ru.klimovich.user_service.util;

public class MessageKeys {
    public static final String USER_EMAIL_ALREADY_EXIST = "User with email [%s] already exist";
    public static final String USER_PHONE_ALREADY_EXIST = "User with phone [%s] already exist";
    public static final String USER_KEYCLOAK_ACCOUNT_EXIST = "User already exist for this Keycloak account";
    public static final String USER_NOT_FOUND = "User not found with id: [%s].";
    public static final String USER_CREATED_SUCCESSFULLY = "The user was successfully created.";
    public static final String USER_UPDATE_SUCCESSFULLY = "The user was successfully updated.";
    public static final String USER_DELETE_SUCCESSFULLY = "The user was successfully deleted.";


    public static final String WISHLIST_CREATED_SUCCESSFULLY = "The wishlist was successfully created";
    public static final String WISHLIST_NAME_ALREADY_EXIST = "The wishlist with name [%s] already exist";
    public static final String WISHLIST_NOT_FOUND = "The wishlist with id [%s] not found.";
    public static final String WISHLIST_UPDATE_SUCCESSFULLY = "The wishlist was successfully updated.";
    public static final String WISHLIST_DELETE_SUCCESSFULLY = "The wishlist was successfully deleted.";


    public static final String PRODUCT_NOT_FOUND = "Product with id [%s] not found.";
    public static final String PRODUCT_ALREADY_IN_WISHLIST = "Product with id [%s] already exists in wishlist.";
    public static final String WISHLIST_ITEM_NOT_FOUND = "Product with id [%s] not found in wishlist.";
    public static final String CATALOG_SERVICE_UNAVAILABLE = "Catalog service is unavailable.";

}
