package ru.klimovich.sellerservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.klimovich.sellerservice.service.SellerService;

@RestController
@RequestMapping("/seller/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class SellerController {

    private final SellerService sellerService;


//    @GetMapping("/me")
//    public SellerResponse getMyAccount(@AuthenticationPrincipal Jwt jwt) {
//        return sellerService.getMyAccount(jwt);
//    }
//
//    @PutMapping("/me")
//    public SellerResponse updateMyAccount(@AuthenticationPrincipal Jwt jwt, @Valid @ModelAttribute SellerUpdateRequest sellerUpdateRequest) {
//        return sellerService.updateMuAccount(jwt.getSubject(), sellerUpdateRequest);
//    }
//
//    @DeleteMapping("/me")
//    public SellerResponse deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
//        sellerService.removeMyAccount(jwt);
//    }
//
//    @PostMapping("/{sellerId}/products/{productId}")
//    public SellerResponse addProduct(@AuthenticationPrincipal Jwt jwt, ProductRequest productId) {
//
//    }
//
//    @DeleteMapping("/{sellerId}/products/{productsId}")
//    public SellerResponse removeProduct(@AuthenticationPrincipal Jwt jwt, Product productId) {
//
//    }
}
