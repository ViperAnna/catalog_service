package ru.seller_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.service.StoreService;

import java.util.List;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class StoreController {
    private final StoreService storeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse createStore(@Valid @RequestBody StoreRequest request) {
        return storeService.createStore(request);
    }

    @GetMapping()
    public List<StoreResponse> getMyStores() {
        return storeService.getMyStores();
    }

    @GetMapping("/{id}")
    public StoreResponse getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id);
    }

    @PutMapping("/{id}")
    public StoreResponse updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return storeService.updateStore(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
    }
}
