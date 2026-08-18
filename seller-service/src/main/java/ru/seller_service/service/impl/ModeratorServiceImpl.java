package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.client.UserGrpcClient;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.event.SellerApprovedEvent;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.mapper.SellerApplicationMapper;
import ru.seller_service.mapper.SellerMapper;
import ru.seller_service.model.Seller;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.model.status.SellerApplicationStatus;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.repository.SellerApplicationRepository;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.service.ModeratorService;
import ru.seller_service.util.MessageKeys;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModeratorServiceImpl implements ModeratorService {

    private final SellerApplicationRepository sellerApplicationRepo;
    private final SellerApplicationMapper sellerApplicationMapper;
    private final SellerMapper sellerMapper;
    private final SellerRepository sellerRepo;
    private final UserGrpcClient userGrpcClient;
    private final OutboxRepository outboxRepo;
    private final EventMapper eventMapper;


    @Override
    @Transactional(readOnly = true)
    public List<SellerApplicationResponse> getPendingApplications() {
        return sellerApplicationRepo
                .findAllBySellerApplicationStatusOrderByCreatedAtAsc(SellerApplicationStatus.PENDING)
                .stream()
                .map(sellerApplicationMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SellerApplicationResponse getApplication(Long applicationId) {
        SellerApplication application = getApplicationById(applicationId);
        return sellerApplicationMapper.toDTO(application);
    }

    @Transactional
    @Override
    public void approve(Long applicationId) {
        SellerApplication application = getApplicationById(applicationId);

        validatePending(application);

        switch (application.getSellerApplicationType()) {
            case REGISTRATION -> approveRegistration(application);
            case UPDATE -> approveUpdate(application);
            case DELETION -> approveDeletion(application);
        }

        SellerApprovedEvent event = SellerApprovedEvent.builder()
                .applicationId(application.getId())
                .keycloakUserId(application.getKeycloakUserId())
                .sellerName(application.getSellerName())
                .email(application.getEmail())
                .build();
        outboxRepo.save(
                OutboxEvent.builder()
                        .id(UUID.randomUUID())
                        .aggregateType("SELLER_APPLICATION")
                        .aggregateId(application.getId().toString())
                        .eventType("SELLER_APPROVED")
                        .payload(eventMapper.toJson(event))
                        .createdAt(LocalDateTime.now())
                        .processed(false)
                        .build()
        );
    }

    @Transactional
    @Override
    public void reject(Long applicationId, ReviewSellerApplicationRequest request) {

        SellerApplication application = getApplicationById(applicationId);

        validatePending(application);

        application.setSellerApplicationStatus(SellerApplicationStatus.REJECTED);
        application.setModeratorComment(request.moderatorComment());

        sellerApplicationRepo.save(application);

    }

    private void approveRegistration(SellerApplication application) {
        validateSellerNotExist(application.getKeycloakUserId());

        Seller seller = sellerMapper.toEntity(application);

        seller.setSellerStatus(SellerStatus.ACTIVE);

        sellerRepo.save(seller);

        userGrpcClient.assignSellerRole(application.getKeycloakUserId());

        application.setSellerApplicationStatus(SellerApplicationStatus.APPROVED);
        sellerApplicationRepo.save(application);
    }

    private void approveUpdate(SellerApplication application) {
        Seller seller = getSellerByKeycloakUserId(application.getKeycloakUserId());

        sellerMapper.updateFromApplication(application, seller);
        sellerRepo.save(seller);

        application.setSellerApplicationStatus(SellerApplicationStatus.APPROVED);
        sellerApplicationRepo.save(application);
    }

    private void approveDeletion(SellerApplication application) {
        Seller seller = getSellerByKeycloakUserId(application.getKeycloakUserId());

        seller.setSellerStatus(SellerStatus.DELETED);
        sellerRepo.save(seller);

        userGrpcClient.removeSellerRole(application.getKeycloakUserId());

        application.setSellerApplicationStatus(SellerApplicationStatus.APPROVED);
        sellerApplicationRepo.save(application);
    }

    private SellerApplication getApplicationById(Long applicationId) {
        return sellerApplicationRepo.findById(applicationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                MessageKeys.APPLICATION_NOT_FOUND,
                                String.valueOf(applicationId)
                        ));
    }

    private Seller getSellerByKeycloakUserId(String userId) {

        return sellerRepo.findByKeycloakUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.SELLER_NOT_FOUND, userId)));
    }

    private void validatePending(SellerApplication application) {
        if (application.getSellerApplicationStatus() != SellerApplicationStatus.PENDING) {
            throw new ResourceConflictException(MessageKeys.APPLICATION_ALREADY_REVIEWED, application.getId());
        }
    }

    private void validateSellerNotExist(String userId) {
        if (sellerRepo.existsByKeycloakUserId(userId)) {
            throw new ResourceConflictException(MessageKeys.SELLER_ALREADY_EXIST, userId);
        }
    }
}
