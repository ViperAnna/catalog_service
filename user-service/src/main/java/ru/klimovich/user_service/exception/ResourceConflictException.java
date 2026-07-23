package ru.klimovich.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ResourceConflictException extends ClientException {

    public ResourceConflictException(String message, Object... args) {
        super(message, args);
    }
}