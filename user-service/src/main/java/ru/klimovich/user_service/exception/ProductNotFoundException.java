package ru.klimovich.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends ClientException {

    public ProductNotFoundException(String message, Object... args) {
        super(message, args);
    }
}
