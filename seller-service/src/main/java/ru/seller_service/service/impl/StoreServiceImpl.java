package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.mapper.StoreMapper;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.repository.StoreRepository;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.service.StoreService;
import ru.seller_service.util.MessageKeys;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {
    private final StoreRepository storeRepo;
    private final StoreMapper storeMapper;
    private final SellerRepository sellerRepo;
    private final CurrentUserService currentUserService;

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

        validateStoreNameUnique(seller.getId(), storeDetails.storeName());

        Store store = storeMapper.toEntity(storeDetails);

        store.setSeller(seller);
        store.setCreatedAt(LocalDateTime.now());
        store.setUpdatedAt(LocalDateTime.now());

        return storeMapper.toDTO(storeRepo.save(store));
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
    public StoreResponse updateStore(Long storeId, StoreRequest storeDetails) {

        Store store = getStore(storeId);

        validateStoreNameUnique(store.getSeller().getId(), storeDetails.storeName());

        storeMapper.updateFromDTO(storeDetails, store);

        return storeMapper.toDTO(storeRepo.save(store));
    }

    @Override
    @Transactional
    public void deleteStore(Long storeId) {

        Store store = getStore(storeId);

        storeRepo.delete(store);
    }


    private Store getStore(Long storeId) {

        Seller seller = getCurrentSeller();

        return storeRepo.findByIdAndSellerId(storeId, seller.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                MessageKeys.STORE_NOT_FOUND, String.valueOf(storeId)));
    }

    private void validateStoreNameUnique(Long sellerId, String storeName) {
        if (storeRepo.existsBySellerIdAndStoreNameIgnoreCase(sellerId, storeName)) {
            throw new ResourceConflictException(MessageKeys.STORE_ALREADY_EXISTS, storeName);
        }
    }


}
