package ru.klimovich.catalog_service.exception;

public class SellerServiceUnavailableException extends RuntimeException {
    public SellerServiceUnavailableException() {
        super("Seller service is unavailable");
    }
}
