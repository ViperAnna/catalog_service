package ru.klimovich.catalog_service.exception;

import java.text.MessageFormat;

public abstract class ClientException extends RuntimeException {
    public ClientException(String message, Object... args) {
        super(MessageFormat.format(message, args));
    }
}
