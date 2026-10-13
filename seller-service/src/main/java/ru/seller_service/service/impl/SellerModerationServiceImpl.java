package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.event.StoreEventType;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.model.status.StoreStatus;
import ru.seller_service.outbox.StoreEventWriter;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.repository.StoreRepository;
import ru.seller_service.service.SellerModerationService;
import ru.seller_service.util.MessageKeys;

/**
 * Блокировка не меняет статусы магазинов и товаров и не удаляет данные:
 * catalog-service скрывает товары по событиям блокировки.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SellerModerationServiceImpl implements SellerModerationService {

    private final SellerRepository sellerRepo;
    private final StoreRepository storeRepo;
    private final StoreEventWriter storeEventWriter;

    @Override
    public void blockSeller(Long sellerId) {
        changeSellerStatus(sellerId, SellerStatus.ACTIVE, SellerStatus.BLOCKED, StoreEventType.SELLER_BLOCKED);
    }

    @Override
    public void unblockSeller(Long sellerId) {
        changeSellerStatus(sellerId, SellerStatus.BLOCKED, SellerStatus.ACTIVE, StoreEventType.SELLER_UNBLOCKED);
    }

    @Override
    public void blockStore(Long storeId) {
        changeStoreStatus(storeId, StoreStatus.ACTIVE, StoreStatus.BLOCKED, StoreEventType.STORE_BLOCKED);
    }

    @Override
    public void unblockStore(Long storeId) {
        changeStoreStatus(storeId, StoreStatus.BLOCKED, StoreStatus.ACTIVE, StoreEventType.STORE_UNBLOCKED);
    }

    @Override
    public void writeStorePurgedEvents(Long sellerId) {
        // Блокировка строк магазинов: подача и одобрение заявок на изменение ждут удаления.
        for (Store store : storeRepo.findAllBySellerIdForUpdate(sellerId)) {
            storeEventWriter.write(store.getId(), sellerId, StoreEventType.STORE_PURGED);
        }
    }

    private void changeSellerStatus(Long sellerId, SellerStatus from, SellerStatus to, StoreEventType eventType) {
        Seller seller = sellerRepo.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.SELLER_NOT_FOUND_BY_ID, sellerId));
        if (seller.getSellerStatus() != from) {
            throw new ResourceConflictException(MessageKeys.SELLER_STATUS_CONFLICT, sellerId, seller.getSellerStatus());
        }
        seller.setSellerStatus(to);
        sellerRepo.save(seller);
        for (Store store : storeRepo.findAllBySellerIdOrderByCreatedAtDesc(sellerId)) {
            storeEventWriter.write(store.getId(), sellerId, eventType);
        }
    }

    private void changeStoreStatus(Long storeId, StoreStatus from, StoreStatus to, StoreEventType eventType) {
        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.STORE_NOT_FOUND, storeId));
        if (store.getStatus() != from) {
            throw new ResourceConflictException(MessageKeys.STORE_STATUS_CONFLICT, storeId, store.getStatus());
        }
        store.setStatus(to);
        storeRepo.save(store);
        storeEventWriter.write(storeId, store.getSeller().getId(), eventType);
    }
}
