package ru.klimovich.catalog_service.exception;

public class ValidationResourceException extends ClientException {
    public ValidationResourceException(String message) {
        super(message);
    }
}
