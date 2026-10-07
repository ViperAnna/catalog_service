package ru.klimovich.notificationservice.exception;

/**
 * SELLER_APPROVED с непустым applicationType, которого notification-service не знает.
 * Письмо не отправляется, KafkaConfig отправляет такое сообщение в DLT без повторов.
 */
public class UnknownApplicationTypeException extends RuntimeException {

    public UnknownApplicationTypeException(Long applicationId, String applicationType) {
        super("Unknown applicationType [%s] in SELLER_APPROVED for application [%s]"
                .formatted(applicationType, applicationId));
    }
}
