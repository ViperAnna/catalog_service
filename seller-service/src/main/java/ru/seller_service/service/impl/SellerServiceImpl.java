package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.dto.responce.SellerResponse;
import ru.seller_service.model.Seller;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.mapper.SellerMapper;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.service.SellerService;
import ru.seller_service.util.MessageKeys;

@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepo;
    private final SellerMapper sellerMapper;
    private final CurrentUserService currentUserService;

    private Seller getCurrentSellerEntity() {
        String keycloakSellerId = currentUserService.getUserId();

        return sellerRepo.findByKeycloakUserId(keycloakSellerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageKeys.SELLER_NOT_FOUND, keycloakSellerId));
    }

    @Transactional(readOnly = true)
    @Override
    public SellerResponse getMyAccount() {
        return sellerMapper.toDTO(getCurrentSellerEntity());
    }
}
