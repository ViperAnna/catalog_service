package ru.seller_service.util;

public class MessageKeys {
    public static final String _EMAIL_ALREADY_EXIST = "User with email [%s] already exist";
    public static final String USER_PHONE_ALREADY_EXIST = "User with phone [%s] already exist";
    public static final String USER_KEYCLOAK_ACCOUNT_EXIST = "User already exist for this Keycloak account";
    public static final String SELLER_NOT_FOUND = "Seller not found for user [%s].";
    public static final String SELLER_ALREADY_EXIST = "Seller already exists for user [%s].";
    public static final String SELLER_NAME_ALREADY_EXISTS = "Seller with name [%s] already exists.";
    public static final String SELLER_NAME_ALREADY_IN_REVIEW = "Seller name [%s] is already in review.";
    public static final String STORE_ALREADY_EXISTS = "Store with name [%s] already exists.";
    public static final String STORE_NOT_FOUND = "Store not found with id: [%s].";
    public static final String INN_ALREADY_IN_REVIEW = "Inn [%s] already in review.";

    public static final String SELLER_IS_NOT_ACTIVE = "Seller with status: [%s]. Seller is not active";
//    public static final String INN_NOT_FOUND = "Inn not found with number: [%s].";
    public static final String INN_ALREADY_EXIST = "Inn with number: [%s] already exist.";
    public static final String APPLICATION_ALREADY_EXISTS = "User already has a pending application.";

    public static final String USER_CREATED_SUCCESSFULLY = "The user was successfully created.";
    public static final String USER_UPDATE_SUCCESSFULLY = "The user was successfully updated.";
    public static final String USER_DELETE_SUCCESSFULLY = "The user was successfully deleted.";


    public static final String APPLICATION_NOT_FOUND = "Application not found with id: [%s].";
    public static final String APPLICATION_ALREADY_REVIEWED = "Application with id: [%s] already reviewed.";
    public static final String SELLER_NOT_FOUND_BY_ID = "Seller not found with id: [%s].";
    public static final String SELLER_STATUS_CONFLICT = "Seller with id: [%s] has status [%s].";
    public static final String STORE_STATUS_CONFLICT = "Store with id: [%s] has status [%s].";
    public static final String STORE_IS_BLOCKED = "Store with id: [%s] is blocked.";
    public static final String STORE_CHANGE_REQUEST_PENDING = "Store with id: [%s] already has a pending change request.";
    public static final String STORE_CHANGE_REQUEST_NOT_FOUND = "Store change request not found with id: [%s].";
    public static final String STORE_CHANGE_REQUEST_ALREADY_REVIEWED = "Store change request with id: [%s] already reviewed.";
    public static final String OWN_APPLICATION_REVIEW_FORBIDDEN = "Moderator can't review own application.";
    public static final String APPLICATION_NOT_FOUND_FOR_USER = "No applications found for user [%s].";


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
