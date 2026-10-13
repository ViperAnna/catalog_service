package ru.seller_service.service;

import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;

import java.util.List;

/**
 * Модерация заявок на изменение магазина.
 */
public interface StoreChangeModerationService {

    List<StoreChangeRequestResponse> getPending();

    StoreChangeRequestResponse getRequest(Long requestId);

    void approve(Long requestId);

    void reject(Long requestId, ReviewSellerApplicationRequest request);
}
