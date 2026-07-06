package ru.klimovich.user_service.exception;

import java.text.MessageFormat;

public class ClientException extends RuntimeException {
    public ClientException(String message, Object... args) {
        super(MessageFormat.format(message, args));
    }
}
