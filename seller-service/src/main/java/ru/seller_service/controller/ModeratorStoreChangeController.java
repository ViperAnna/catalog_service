package ru.seller_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.service.StoreChangeModerationService;

import java.util.List;

@RestController
@RequestMapping("/moderator/store-change-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorStoreChangeController {

    private final StoreChangeModerationService moderationService;

    @GetMapping("/pending")
    public List<StoreChangeRequestResponse> getPending() {
        return moderationService.getPending();
    }

    @GetMapping("/{id}")
    public StoreChangeRequestResponse getRequest(@PathVariable Long id) {
        return moderationService.getRequest(id);
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long id) {
        moderationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id, @RequestBody @Valid ReviewSellerApplicationRequest request) {
        moderationService.reject(id, request);
    }
}
