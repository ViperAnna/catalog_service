package ru.seller_service.service;

import ru.seller_service.dto.request.seller.SellerDeletionRequest;
import ru.seller_service.dto.request.seller.SellerRegistrationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.dto.request.seller.SellerUpdateRequest;

import java.util.List;


public interface SellerApplicationService {

    SellerApplicationResponse createRegistrationRequest(SellerRegistrationRequest applicationDetails);

    SellerApplicationResponse createUpdateRequest(SellerUpdateRequest sellerUpdateDetails);
    SellerApplicationResponse createDeletionRequest(SellerDeletionRequest sellerDeletionDetails);
    SellerApplicationResponse getMyCurrentApplication();
    List<SellerApplicationResponse> getMyApplicationHistory();

//    SellerApplicationResponse canceledCurrentApplication();






}
