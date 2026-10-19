package ru.klimovich.notificationservice.exception;

/**
 * Неизвестное или некорректное сообщение order-events-topic. Повторять бессмысленно:
 * KafkaConfig отправляет его в DLT без повторов. Сообщение исключения не содержит payload и email.
 */
public class InvalidOrderEventException extends RuntimeException {

    public InvalidOrderEventException(String reason) {
        super(reason);
    }
}
