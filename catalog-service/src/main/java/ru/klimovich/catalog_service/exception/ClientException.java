package ru.klimovich.catalog_service.exception;

public abstract class ClientException extends RuntimeException {
    public ClientException(String message, Object... args) {
        super(args.length == 0
                ? message
                : String.format(message, args));
    }
}
