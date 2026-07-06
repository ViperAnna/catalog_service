package ru.klimovich.user_service.exception;

public class ResourceConflictException extends ClientException {
    public ResourceConflictException(String message) {
        super(message);
    }
}