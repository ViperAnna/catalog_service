package ru.seller_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.service.ModeratorService;

import java.util.List;

@RestController
@RequestMapping("/moderator/seller-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorController {
    private final ModeratorService moderatorService;

    @GetMapping("/pending")
    public List<SellerApplicationResponse> getPending() {
        return moderatorService.getPendingApplications();
    }

    @GetMapping("/{id}")
    public SellerApplicationResponse getApplication(@PathVariable Long id) {
        return moderatorService.getApplication(id);
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long id) {
        moderatorService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id, @RequestBody @Valid ReviewSellerApplicationRequest request) {
        moderatorService.reject(id, request);
    }
}
