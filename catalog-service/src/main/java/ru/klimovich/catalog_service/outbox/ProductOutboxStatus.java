package ru.klimovich.catalog_service.outbox;

/**
 * PENDING — ждет публикации; SENT — опубликовано; DISCARDED — товар так и не был удален
 * (событие не публикуется, повторное удаление вернет PENDING).
 */
public enum ProductOutboxStatus {
    PENDING,
    SENT,
    DISCARDED
}
