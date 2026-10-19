package ru.seller_service.service;

import org.springframework.stereotype.Service;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.request.store.StoreUpdateRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.dto.responce.StoreResponse;

import java.util.List;

@Service
public interface StoreService {

    StoreResponse createStore(StoreRequest request);

    List<StoreResponse> getMyStores();

    StoreResponse getStoreById(Long storeId);

    /** Создает заявку на изменение магазина; магазин меняется только после одобрения модератором. */
    StoreChangeRequestResponse updateStore(Long storeId, StoreUpdateRequest storeDetails);

    List<StoreChangeRequestResponse> getChangeRequests(Long storeId);

    void deleteStore(Long storeId);
}
