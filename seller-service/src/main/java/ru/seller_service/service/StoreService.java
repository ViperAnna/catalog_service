package ru.seller_service.service;

import org.springframework.stereotype.Service;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.responce.StoreResponse;

import java.util.List;

@Service
public interface StoreService {

    StoreResponse createStore(StoreRequest request);

    List<StoreResponse> getMyStores();

    StoreResponse getStoreById(Long storeId);

    StoreResponse updateStore(Long storeId, StoreRequest storeDetails);

    void deleteStore(Long storeId);
}
