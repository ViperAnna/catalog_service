package ru.klimovich.sellerservice.service;

import org.springframework.stereotype.Service;
import ru.klimovich.sellerservice.dto.request.SellerApplicationRequest;
import ru.klimovich.sellerservice.dto.responce.SellerApplicationResponse;

import java.util.List;

@Service
public interface ModeratorService {

    SellerApplicationResponse create(SellerApplicationRequest sellerApplicationRequest);

    List<SellerApplicationResponse> getPending();

    SellerApplicationResponse getMyApplication();

    void approve(Long applicationId);

    void reject(Long applicationId, String reason);
}
