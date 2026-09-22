package ru.seller_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends ClientException {

    public ResourceNotFoundException(String message, Object... args) {
        super(message, args);
    }
}