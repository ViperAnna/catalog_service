package ru.seller_service.event;

/**
 * Типы событий магазина, публикуемых в store-events-topic.
 * STORE_DELETED больше не публикуется (заменен STORE_PURGED), значение оставлено
 * для записей outbox, созданных до change seller-store-lifecycle.
 */
public enum StoreEventType {
    STORE_DELETED,
    STORE_PURGED,
    STORE_BLOCKED,
    STORE_UNBLOCKED,
    SELLER_BLOCKED,
    SELLER_UNBLOCKED
}
