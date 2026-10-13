package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import ru.seller_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.request.store.StoreUpdateRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.model.StoreChangeRequest;
import ru.seller_service.model.status.StoreChangeRequestStatus;
import ru.seller_service.repository.StoreChangeRequestRepository;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.event.StoreEventType;
import ru.seller_service.mapper.StoreMapper;
import ru.seller_service.outbox.StoreEventWriter;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.model.status.StoreStatus;
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
    private static final String PENDING_CHANGE_REQUEST_INDEX = "uq_store_change_requests_pending";
    private final StoreRepository storeRepo;
    private final StoreMapper storeMapper;
    private final SellerRepository sellerRepo;
    private final CurrentUserService currentUserService;
    private final StoreEventWriter storeEventWriter;
    private final StoreChangeRequestRepository changeRequestRepo;

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
        validateSellerNotBlocked(seller);
        String storeName = storeDetails.storeName().trim();

        validateStoreNameUnique(seller.getId(), storeName, null);

        Store store = storeMapper.toEntity(storeDetails);
        store.setStoreName(storeName);
        store.setPhoneNumber(PhoneNormalizer.normalize(store.getPhoneNumber()));

        store.setSeller(seller);
        store.setStatus(StoreStatus.ACTIVE);
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

    /**
     * PUT /stores/{id} создает заявку на изменение; магазин меняется только после одобрения
     * модератором. Магазин читается с блокировкой строки (D3b), одна PENDING-заявка на магазин
     * гарантируется проверкой и частичным уникальным индексом uq_store_change_requests_pending.
     */
    @Override
    @Transactional
    public StoreChangeRequestResponse updateStore(Long storeId, StoreUpdateRequest storeDetails) {

        Store store = getModifiableStore(storeId);
        if (changeRequestRepo.existsByStoreIdAndStatus(storeId, StoreChangeRequestStatus.PENDING)) {
            throw new ResourceConflictException(MessageKeys.STORE_CHANGE_REQUEST_PENDING, storeId);
        }
        String storeName = storeDetails.storeName().trim();

        validateStoreNameUnique(store.getSeller().getId(), storeName, store.getId());

        StoreChangeRequest request = StoreChangeRequest.builder()
                .store(store)
                .keycloakUserId(currentUserService.getUserId())
                .storeName(storeName)
                .description(storeDetails.description())
                .email(storeDetails.email())
                .phoneNumber(PhoneNormalizer.normalize(storeDetails.phoneNumber()))
                .status(StoreChangeRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return storeMapper.toChangeRequestDTO(saveChangeRequest(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreChangeRequestResponse> getChangeRequests(Long storeId) {
        Store store = getStore(storeId);
        return changeRequestRepo.findAllByStoreIdOrderByCreatedAtDesc(store.getId())
                .stream()
                .map(storeMapper::toChangeRequestDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteStore(Long storeId) {

        Store store = getModifiableStore(storeId);

        // Заявки магазина, включая PENDING, удаляются каскадом (Store.changeRequests).
        storeRepo.delete(store);

        storeEventWriter.write(store.getId(), store.getSeller().getId(), StoreEventType.STORE_PURGED);
    }

    /**
     * Изменять и удалять можно только магазин ACTIVE-продавца в статусе ACTIVE.
     * Статусы проверяются в момент операции; строка магазина блокируется (D3b).
     */
    private Store getModifiableStore(Long storeId) {
        Seller seller = getCurrentSeller();
        Store store = storeRepo.findByIdAndSellerIdForUpdate(storeId, seller.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                MessageKeys.STORE_NOT_FOUND, String.valueOf(storeId)));
        validateSellerNotBlocked(store.getSeller());
        if (store.getStatus() == StoreStatus.BLOCKED) {
            throw new ResourceConflictException(MessageKeys.STORE_IS_BLOCKED, storeId);
        }
        return store;
    }

    private void validateSellerNotBlocked(Seller seller) {
        if (seller.getSellerStatus() == SellerStatus.BLOCKED) {
            throw new ResourceConflictException(MessageKeys.SELLER_IS_NOT_ACTIVE, seller.getSellerStatus().name());
        }
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

    /** Одновременная подача: вторая PENDING-заявка отклоняется индексом и становится 409. */
    private StoreChangeRequest saveChangeRequest(StoreChangeRequest request) {
        try {
            return changeRequestRepo.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolations.isUniqueViolation(e, PENDING_CHANGE_REQUEST_INDEX)) {
                throw new ResourceConflictException(MessageKeys.STORE_CHANGE_REQUEST_PENDING, request.getStore().getId());
            }
            throw e;
        }
    }



}
