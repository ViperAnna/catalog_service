package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import ru.seller_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.request.store.StoreUpdateRequest;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.event.StoreDeletedEvent;
import ru.seller_service.event.StoreEventType;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.mapper.StoreMapper;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.repository.StoreRepository;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.service.StoreService;
import ru.seller_service.util.MessageKeys;
import ru.seller_service.util.PhoneNormalizer;
import ru.seller_service.util.UniqueViolations;

import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final String STORE_NAME_UNIQUE_INDEX = "uq_stores_seller_store_name";
    private final StoreRepository storeRepo;
    private final StoreMapper storeMapper;
    private final SellerRepository sellerRepo;
    private final CurrentUserService currentUserService;
    private final OutboxRepository outboxRepo;
    private final EventMapper eventMapper;

    private Seller getCurrentSeller() {

        String keycloakUserId = currentUserService.getUserId();

        return sellerRepo.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                MessageKeys.SELLER_NOT_FOUND,
                                keycloakUserId
                        ));
    }

    @Override
    @Transactional
    public StoreResponse createStore(StoreRequest storeDetails) {

        Seller seller = getCurrentSeller();
        String storeName = storeDetails.storeName().trim();

        validateStoreNameUnique(seller.getId(), storeName, null);

        Store store = storeMapper.toEntity(storeDetails);
        store.setStoreName(storeName);
        store.setPhoneNumber(PhoneNormalizer.normalize(store.getPhoneNumber()));

        store.setSeller(seller);
        store.setCreatedAt(LocalDateTime.now());
        store.setUpdatedAt(LocalDateTime.now());

        return storeMapper.toDTO(saveStore(store));
    }

    @Override
    public List<StoreResponse> getMyStores() {

        Seller seller = getCurrentSeller();

        return storeRepo.findAllBySellerIdOrderByCreatedAtDesc(seller.getId())
                .stream()
                .map(storeMapper::toDTO)
                .toList();
    }

    @Override
    public StoreResponse getStoreById(Long storeId) {
        return storeMapper.toDTO(getStore(storeId));
    }

    @Override
    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest storeDetails) {

        Store store = getStore(storeId);
        String storeName = storeDetails.storeName().trim();

        validateStoreNameUnique(store.getSeller().getId(), storeName, store.getId());

        storeMapper.updateFromDTO(storeDetails, store);
        store.setStoreName(storeName);
        if (storeDetails.phoneNumber() != null) {
            store.setPhoneNumber(PhoneNormalizer.normalize(storeDetails.phoneNumber()));
        }

        return storeMapper.toDTO(saveStore(store));
    }

    @Override
    @Transactional
    public void deleteStore(Long storeId) {

        Store store = getStore(storeId);

        storeRepo.delete(store);

        StoreDeletedEvent event = StoreDeletedEvent.builder()
                .storeId(store.getId())
                .sellerId(store.getSeller().getId())
                .build();
        outboxRepo.save(
                OutboxEvent.builder()
                        .aggregateType("STORE")
                        .aggregateId(store.getId().toString())
                        .eventType(StoreEventType.STORE_DELETED.name())
                        .payload(eventMapper.toJson(event))
                        .createdAt(LocalDateTime.now())
                        .processed(false)
                        .build()
        );
    }


    private Store getStore(Long storeId) {

        Seller seller = getCurrentSeller();

        return storeRepo.findByIdAndSellerId(storeId, seller.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                MessageKeys.STORE_NOT_FOUND, String.valueOf(storeId)));
    }

    private void validateStoreNameUnique(Long sellerId, String storeName, Long excludeStoreId) {
        if (storeRepo.existsSameName(sellerId, storeName, excludeStoreId)) {
            throw new ResourceConflictException(MessageKeys.STORE_ALREADY_EXISTS, storeName);
        }
    }

    /**
     * Сохраняет магазин сразу (flush), чтобы нарушение уникального индекса имени
     * при гонке возникло здесь и стало 409, а не 500 при commit.
     */
    private Store saveStore(Store store) {
        try {
            return storeRepo.saveAndFlush(store);
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolations.isUniqueViolation(e, STORE_NAME_UNIQUE_INDEX)) {
                throw new ResourceConflictException(MessageKeys.STORE_ALREADY_EXISTS, store.getStoreName());
            }
            throw e;
        }
    }



}
