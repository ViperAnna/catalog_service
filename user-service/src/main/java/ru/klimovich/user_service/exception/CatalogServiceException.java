package ru.klimovich.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CatalogServiceException extends ClientException {

    public CatalogServiceException(String message, Object... args) {
        super(message, args);
    }
}
