package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.dto.request.seller.SellerDeletionRequest;
import ru.seller_service.dto.request.seller.SellerRegistrationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.event.SellerApplicationCreatedEvent;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.model.Seller;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.model.status.SellerApplicationStatus;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.dto.request.seller.SellerUpdateRequest;
import ru.seller_service.mapper.SellerApplicationMapper;
import ru.seller_service.repository.SellerApplicationRepository;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.service.SellerApplicationService;
import ru.seller_service.util.MessageKeys;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerApplicationServiceImpl implements SellerApplicationService {
    private final CurrentUserService currentUserService;
    private final SellerApplicationRepository sellerApplicationRepo;
    private final SellerApplicationMapper sellerApplicationMapper;
    private final SellerRepository sellerRepo;
    private final OutboxRepository outboxRepo;
    private final EventMapper eventMapper;


    private Seller getSellerByKeycloakUserId(String userId) {

        return sellerRepo.findByKeycloakUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.SELLER_NOT_FOUND, userId)));
    }

    @Override
    public SellerApplicationResponse createRegistrationRequest(SellerRegistrationRequest applicationDetails) {
        String userId = currentUserService.getUserId();

        validateSellerNotExist(userId);
        validateNoPendingApplication(userId);
        validateInnUnique(applicationDetails.inn());

        SellerApplication application = sellerApplicationMapper.toEntity(applicationDetails);
        application.setKeycloakUserId(userId);
        application.setSellerApplicationStatus(SellerApplicationStatus.PENDING);
        application.setSellerApplicationType(SellerApplicationType.REGISTRATION);
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        sellerApplicationRepo.save(application);

        SellerApplicationCreatedEvent event = SellerApplicationCreatedEvent.builder()
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
                        .eventType("SELLER_APPLICATION_CREATED")
                        .payload(eventMapper.toJson(event))
                        .createdAt(LocalDateTime.now())
                        .processed(false)
                        .build()
        );

        return sellerApplicationMapper.toDTO(application);
    }

    @Override
    public SellerApplicationResponse createUpdateRequest(SellerUpdateRequest sellerUpdateDetails) {
        String userId = currentUserService.getUserId();
        Seller seller = getSellerByKeycloakUserId(userId);

        validateSellerActive(seller);
        validateNoPendingApplication(userId);

        SellerApplication application = sellerApplicationMapper.toEntity(sellerUpdateDetails);

        application.setKeycloakUserId(userId);
        application.setSellerName(seller.getSellerName());
        application.setInn(seller.getInn());
        application.setSellerApplicationStatus(SellerApplicationStatus.PENDING);
        application.setSellerApplicationType(SellerApplicationType.UPDATE);
        application.setUpdatedAt(LocalDateTime.now());

        sellerApplicationRepo.save(application);

        return sellerApplicationMapper.toDTO(application);
    }

    @Transactional
    @Override
    public SellerApplicationResponse createDeletionRequest(SellerDeletionRequest sellerDeletionDetails) {
        String userId = currentUserService.getUserId();
        Seller seller = getSellerByKeycloakUserId(userId);

        validateSellerActive(seller);
        validateNoPendingApplication(userId);

        SellerApplication application = SellerApplication.builder()
                .keycloakUserId(userId)
                .sellerName(seller.getSellerName())
                .inn(seller.getInn())
                .phoneNumber(seller.getPhoneNumber())
                .email(seller.getEmail())
                .paymentAccount(seller.getPaymentAccount())
                .sellerApplicationStatus(SellerApplicationStatus.PENDING)
                .sellerApplicationType(SellerApplicationType.DELETION)
                .requestReason(sellerDeletionDetails.reason())
                .build();
        sellerApplicationRepo.save(application);

        return sellerApplicationMapper.toDTO(application);
    }

    @Override
    public SellerApplicationResponse getMyCurrentApplication() {
        String userId = currentUserService.getUserId();

        SellerApplication application = sellerApplicationRepo.findFirstByKeycloakUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(MessageKeys.APPLICATION_NOT_FOUND, userId));
        return sellerApplicationMapper.toDTO(application);
    }



    @Override
    public List<SellerApplicationResponse> getMyApplicationHistory() {
        String userId = currentUserService.getUserId();

        return sellerApplicationRepo.findAllByKeycloakUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(sellerApplicationMapper::toDTO)
                .toList();
    }

//    @Override
//    public SellerApplicationResponse canceledCurrentApplication() {
//        return null;
//    }

    private void validateSellerNotExist(String userId) {
        if (sellerRepo.existsByKeycloakUserId(userId)) {
            throw new ResourceConflictException(MessageKeys.SELLER_ALREADY_EXIST, userId);
        }
    }

    private void validateNoPendingApplication(String userId) {
        if (sellerApplicationRepo.existsByKeycloakUserIdAndSellerApplicationStatus(userId, SellerApplicationStatus.PENDING)) {
            throw new ResourceConflictException(MessageKeys.APPLICATION_ALREADY_EXISTS);
        }
    }

    private void validateInnUnique(String inn) {
        if (sellerRepo.existsByInn(inn)) {
            throw new ResourceConflictException(MessageKeys.INN_ALREADY_EXIST, inn);
        }
        if (sellerApplicationRepo.existsByInnAndSellerApplicationStatus(
                inn,
                SellerApplicationStatus.PENDING)) {

            throw new ResourceConflictException(
                    MessageKeys.INN_ALREADY_IN_REVIEW,
                    inn);
        }
    }

    private void validateSellerActive(Seller seller) {
        if (seller.getSellerStatus() != SellerStatus.ACTIVE) {
            throw new ResourceConflictException(MessageKeys.SELLER_IS_NOT_ACTIVE, seller.getSellerStatus().name());
        }
    }
}
