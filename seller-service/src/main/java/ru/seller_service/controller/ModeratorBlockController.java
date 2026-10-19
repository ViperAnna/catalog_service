package ru.seller_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.seller_service.service.SellerModerationService;

@RestController
@RequestMapping("/moderator")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MODERATOR')")
public class ModeratorBlockController {

    private final SellerModerationService moderationService;

    @PostMapping("/sellers/{sellerId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockSeller(@PathVariable Long sellerId) {
        moderationService.blockSeller(sellerId);
    }

    @PostMapping("/sellers/{sellerId}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockSeller(@PathVariable Long sellerId) {
        moderationService.unblockSeller(sellerId);
    }

    @PostMapping("/stores/{storeId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockStore(@PathVariable Long storeId) {
        moderationService.blockStore(storeId);
    }

    @PostMapping("/stores/{storeId}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblockStore(@PathVariable Long storeId) {
        moderationService.unblockStore(storeId);
    }
}
