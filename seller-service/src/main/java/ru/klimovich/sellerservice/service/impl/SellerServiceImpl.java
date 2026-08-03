package ru.klimovich.sellerservice.service.impl;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import ru.klimovich.sellerservice.dto.responce.SellerResponse;
import ru.klimovich.sellerservice.model.Seller;
import ru.klimovich.sellerservice.service.SellerService;

@Service
public class SellerServiceImpl implements SellerService {

//    @Override
//    public SellerResponse createSeller(SellerResponse sellerDetails) {
//        Seller seller = new Seller();
//
//        return null;
//    }
//
//    @Override
//    public SellerResponse getMyAccount(Jwt jwt) {
//        return null;
//    }
//
//    @Override
//    public SellerResponse updateMuAccount(Jwt jwt, SellerUpdateRequest sellerUpdateRequest) {
//        return null;
//    }
//
//    @Override
//    public SellerResponse getProducts() {
//        return null;
//    }
//
//    @Override
//    public void removeMyAccount(Jwt jwt) {
//
//    }
//
//    @Override
//    public SellerResponse addProduct(Jwt jwt, ProductRequest productDetails) {
//        return null;
//    }
//
//    @Override
//    public SellerResponse removeProduct(Jwt jwt, ProductRequest productDetails) {
//        return null;
//    }

    @Override
    public Seller getByUserid(String keycloakUserId) {
        return null;
    }

    @Override
    public SellerResponse getCurrentSeller() {
        return null;
    }
}
