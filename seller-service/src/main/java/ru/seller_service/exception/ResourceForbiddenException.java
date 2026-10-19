package ru.seller_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ResourceForbiddenException extends ClientException {

    public ResourceForbiddenException(String message, Object... args) {
        super(message, args);
    }
}
