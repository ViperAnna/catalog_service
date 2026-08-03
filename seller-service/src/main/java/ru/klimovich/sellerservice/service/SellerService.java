package ru.klimovich.sellerservice.service;

import ru.klimovich.sellerservice.dto.responce.SellerResponse;
import ru.klimovich.sellerservice.model.Seller;

public interface SellerService {

//    SellerResponse createSeller(SellerResponse sellerDetails);

//    SellerResponse getMyAccount(Jwt jwt);
//
//    SellerResponse updateMuAccount(Jwt jwt, SellerUpdateRequest sellerUpdateRequest);
//
//    SellerResponse getProducts();
//
//    void removeMyAccount(Jwt jwt);
//
//    SellerResponse addProduct(Jwt jwt, ProductRequest productDetails);
//
//    SellerResponse removeProduct(Jwt jwt, ProductRequest productDetails);

    Seller getByUserid(String keycloakUserId);

    SellerResponse getCurrentSeller();

}
