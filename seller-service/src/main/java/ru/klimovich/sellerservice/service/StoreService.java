package ru.klimovich.sellerservice.service;

import org.springframework.stereotype.Service;
import ru.klimovich.sellerservice.dto.request.StoreRequest;
import ru.klimovich.sellerservice.dto.responce.StoreResponse;

import java.util.List;

@Service
public interface StoreService {

    StoreResponse createStore(StoreRequest request);

    List<StoreResponse> getMyShops();

    StoreResponse getShopById(Long storeId);

    StoreResponse updateStore(Long storeId, StoreRequest storeDetails);

    void removeStore(Long storeId);
}
