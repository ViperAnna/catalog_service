package ru.seller_service.service;

/**
 * Блокировка и разблокировка продавцов и магазинов модератором.
 */
public interface SellerModerationService {

    void blockSeller(Long sellerId);

    void unblockSeller(Long sellerId);

    void blockStore(Long storeId);

    void unblockStore(Long storeId);

    /** STORE_PURGED на каждый магазин продавца перед его окончательным удалением. */
    void writeStorePurgedEvents(Long sellerId);
}
