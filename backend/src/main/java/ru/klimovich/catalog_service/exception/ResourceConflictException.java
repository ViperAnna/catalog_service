package ru.klimovich.catalog_service.exception;

public class ResourceConflictException extends ClientException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
