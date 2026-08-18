package ru.seller_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.seller_service.dto.responce.SellerResponse;
import ru.seller_service.service.SellerService;

@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/me")
    public SellerResponse getMyAccount() {
        return sellerService.getMyAccount();
    }
}
