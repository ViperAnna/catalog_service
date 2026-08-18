package ru.seller_service.service;

import org.springframework.stereotype.Service;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;

import java.util.List;

@Service
public interface ModeratorService {
    List<SellerApplicationResponse> getPendingApplications();

    SellerApplicationResponse getApplication(Long applicationId);

    void approve(Long applicationId);

    void reject(Long applicationId, ReviewSellerApplicationRequest request);

}
