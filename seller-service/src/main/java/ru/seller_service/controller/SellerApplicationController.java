package ru.seller_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.seller_service.dto.request.seller.SellerDeletionRequest;
import ru.seller_service.dto.request.seller.SellerUnblockRequest;
import ru.seller_service.dto.request.seller.SellerRegistrationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.dto.request.seller.SellerUpdateRequest;
import ru.seller_service.service.SellerApplicationService;

import java.util.List;

@RestController
@RequestMapping("/seller-applications")
@RequiredArgsConstructor
public class SellerApplicationController {
    private final SellerApplicationService sellerApplicationService;


    @PostMapping("/registration")
    public ResponseEntity<SellerApplicationResponse> createSellerApplication(@Valid @RequestBody SellerRegistrationRequest applicationDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerApplicationService.createRegistrationRequest(applicationDetails));
    }

    @GetMapping("/me")
    public ResponseEntity<SellerApplicationResponse> getMyApplication() {
        return ResponseEntity.ok(
                sellerApplicationService.getMyCurrentApplication()
        );
    }

    @PutMapping("/update")
    public ResponseEntity<SellerApplicationResponse> updateMyAccount(@Valid @ModelAttribute SellerUpdateRequest applicationDetails) {
        return ResponseEntity.ok(
                sellerApplicationService.createUpdateRequest(applicationDetails));
    }

    @GetMapping("/history")
    public ResponseEntity<List<SellerApplicationResponse>> getMyHistory() {
        return ResponseEntity.ok(
                sellerApplicationService.getMyApplicationHistory()
        );
    }

    @PostMapping("/unblock")
    public ResponseEntity<SellerApplicationResponse> createUnblockRequest(@RequestBody @Valid SellerUnblockRequest applicationDetails) {
        return ResponseEntity.ok(
                sellerApplicationService.createUnblockRequest(applicationDetails)
        );
    }

    @DeleteMapping("/deletion")
    public ResponseEntity<SellerApplicationResponse> createDeletionRequest(@RequestBody @Valid SellerDeletionRequest applicationDetails) {
        return ResponseEntity.ok(
                sellerApplicationService.createDeletionRequest(applicationDetails)
        );
    }
}
