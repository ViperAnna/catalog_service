package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.seller_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.seller_service.client.UserGrpcClient;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.event.SellerApprovedEvent;
import ru.seller_service.event.SellerEventType;
import ru.seller_service.event.SellerRejectedEvent;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceForbiddenException;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.service.SellerModerationService;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.mapper.SellerApplicationMapper;
import ru.seller_service.mapper.SellerMapper;
import ru.seller_service.model.Seller;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.model.status.SellerApplicationStatus;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRedaction;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.repository.SellerApplicationRepository;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.service.ModeratorService;
import ru.seller_service.util.MessageKeys;
import ru.seller_service.util.UniqueViolations;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ModeratorServiceImpl implements ModeratorService {

    private static final String SELLER_APPLICATION = "SELLER_APPLICATION";

    private static final String SELLER_NAME_UNIQUE_INDEX = "uq_sellers_seller_name";
    /** Имя ограничения UNIQUE (seller_name), которое генерирует Hibernate для Seller.sellerName. */
    private static final String SELLER_NAME_HIBERNATE_CONSTRAINT = "uk8757g69e614binmjpvpoeda7d";

    private final SellerApplicationRepository sellerApplicationRepo;
    private final SellerApplicationMapper sellerApplicationMapper;
    private final SellerMapper sellerMapper;
    private final SellerRepository sellerRepo;
    private final UserGrpcClient userGrpcClient;
    private final OutboxRepository outboxRepo;
    private final EventMapper eventMapper;
    private final CurrentUserService currentUserService;
    private final SellerModerationService sellerModerationService;

    private void saveOutbox(SellerApplication application, SellerEventType eventType, Object payload) {
        outboxRepo.save(OutboxEvent.builder()
                .aggregateType(SELLER_APPLICATION)
                .aggregateId(application.getId().toString())
                .eventType(eventType.name())
                .payload(eventMapper.toJson(payload))
                .createdAt(LocalDateTime.now())
                .processed(false)
                .build());
    }


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

    @Override
    public void approve(Long applicationId) {
        SellerApplication application = getApplicationById(applicationId);

        validateNotOwnApplication(application);
        validatePending(application);

        switch (application.getSellerApplicationType()) {
            case REGISTRATION -> approveRegistration(application);
            case UPDATE -> approveUpdate(application);
            case DELETION -> approveDeletion(application);
            case UNBLOCK -> approveUnblock(application);
        }

        SellerApprovedEvent event = SellerApprovedEvent.builder()
                .applicationId(application.getId())
                .keycloakUserId(application.getKeycloakUserId())
                .sellerName(application.getSellerName())
                .email(application.getEmail())
                .applicationType(application.getSellerApplicationType())
                .build();

        saveOutbox(
                application,
                SellerEventType.SELLER_APPROVED,
                event
        );
    }

    @Override
    public void reject(Long applicationId, ReviewSellerApplicationRequest request) {

        SellerApplication application = getApplicationById(applicationId);

        validateNotOwnApplication(application);
        validatePending(application);

        application.setSellerApplicationStatus(SellerApplicationStatus.REJECTED);
        application.setModeratorComment(request.moderatorComment());

        SellerRejectedEvent event = SellerRejectedEvent.builder()
                .applicationId(application.getId())
                .keycloakUserId(application.getKeycloakUserId())
                .sellerName(application.getSellerName())
                .email(application.getEmail())
                .moderatorComment(request.moderatorComment())
                .build();

        saveOutbox(
                application,
                SellerEventType.SELLER_REJECTED,
                event);

    }

    /**
     * Продавец получает отдельный аккаунт только с ролью SELLER (change separate-seller-account):
     * Seller записывается до gRPC (keycloakUserId временно — заявитель), затем user-service создает
     * аккаунт, и keycloakUserId указывает на него. Аккаунт заявителя не меняется. Если транзакция
     * после создания аккаунта откатится, аккаунт удаляется (компенсация).
     */
    private void approveRegistration(SellerApplication application) {
        String applicantId = application.getKeycloakUserId();
        validateSellerNotExist(applicantId);
        validateSellerNameUnique(application.getSellerName());

        Seller seller = sellerMapper.toEntity(application);
        seller.setApplicantKeycloakUserId(applicantId);
        seller.setSellerStatus(SellerStatus.ACTIVE);

        saveNewSeller(seller);

        String sellerAccountId = userGrpcClient.createSellerAccount(application.getEmail(), application.getSellerName());
        compensateOnRollback(sellerAccountId);
        seller.setKeycloakUserId(sellerAccountId);
        sellerRepo.save(seller);

        application.setSellerApplicationStatus(SellerApplicationStatus.APPROVED);
        sellerApplicationRepo.save(application);
    }

    /**
     * Удаление созданного аккаунта продавца, если транзакция одобрения не зафиксирована.
     * Ошибка компенсации пишется в лог: повтор одобрения переиспользует незавершенный аккаунт.
     */
    private void compensateOnRollback(String sellerAccountId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    userGrpcClient.deleteSellerAccount(sellerAccountId);
                } catch (RuntimeException e) {
                    log.error("Compensation failed: seller account {} was not deleted", sellerAccountId, e);
                }
            }
        });
    }

    private void approveUpdate(SellerApplication application) {
        Seller seller = getSellerByKeycloakUserId(application.getKeycloakUserId());
        // Текущий статус, а не статус при подаче: заявка заблокированного продавца остается PENDING.
        if (seller.getSellerStatus() == SellerStatus.BLOCKED) {
            throw new ResourceConflictException(MessageKeys.SELLER_IS_NOT_ACTIVE, seller.getSellerStatus().name());
        }

        sellerMapper.updateFromApplication(application, seller);
        sellerRepo.save(seller);

        application.setSellerApplicationStatus(SellerApplicationStatus.APPROVED);
        sellerApplicationRepo.save(application);
    }

    /**
     * Разблокировка тем же путем, что и действием модератора: проверка BLOCKED (иначе 409)
     * и SELLER_UNBLOCKED для каждого магазина. Отдельные блокировки магазинов не снимаются.
     */
    private void approveUnblock(SellerApplication application) {
        Seller seller = getSellerByKeycloakUserId(application.getKeycloakUserId());

        sellerModerationService.unblockSeller(seller.getId());

        application.setSellerApplicationStatus(SellerApplicationStatus.APPROVED);
        sellerApplicationRepo.save(application);
    }

    /**
     * Окончательное удаление продавца (ACTIVE или BLOCKED) в одной транзакции:
     * STORE_PURGED на каждый магазин (товары и изображения удалит catalog-service),
     * обезличивание уже опубликованных записей outbox его заявок, физическое удаление
     * заявок и Seller (магазины удаляются каскадом). SELLER_APPROVED записывается после
     * в approve() и сохраняет payload до публикации, чтобы письмо дошло.
     * Ошибка gRPC откатывает все изменения.
     */
    private void approveDeletion(SellerApplication application) {
        String keycloakUserId = application.getKeycloakUserId();
        Seller seller = getSellerByKeycloakUserId(keycloakUserId);
        boolean separateAccount = hasSeparateAccount(seller);

        List<SellerApplication> applications = new ArrayList<>(
                sellerApplicationRepo.findAllByKeycloakUserIdOrderByCreatedAtDesc(keycloakUserId));
        if (separateAccount) {
            // Регистрационные заявки заявителя содержат персональные данные продавца.
            sellerApplicationRepo.findAllByKeycloakUserIdOrderByCreatedAtDesc(seller.getApplicantKeycloakUserId())
                    .stream()
                    .filter(a -> a.getSellerApplicationType() == SellerApplicationType.REGISTRATION)
                    .forEach(applications::add);
        }

        sellerModerationService.writeStorePurgedEvents(seller.getId());
        redactPublishedOutbox(applications);

        sellerApplicationRepo.deleteAll(applications);
        sellerRepo.delete(seller);

        if (separateAccount) {
            userGrpcClient.deleteSellerAccount(keycloakUserId);
        } else {
            // Продавец, созданный до change separate-seller-account, работает на аккаунте покупателя.
            userGrpcClient.removeSellerRole(keycloakUserId);
        }
    }

    private static boolean hasSeparateAccount(Seller seller) {
        return seller.getApplicantKeycloakUserId() != null
                && !seller.getApplicantKeycloakUserId().equals(seller.getKeycloakUserId());
    }

    private void redactPublishedOutbox(List<SellerApplication> applications) {
        List<String> applicationIds = applications.stream()
                .map(a -> String.valueOf(a.getId()))
                .toList();
        if (applicationIds.isEmpty()) {
            return;
        }
        outboxRepo.findByAggregateTypeAndAggregateIdInAndProcessedTrue(SELLER_APPLICATION, applicationIds)
                .forEach(event -> OutboxRedaction.redact(event, eventMapper));
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

    /**
     * Модератор не рассматривает свою заявку: иначе он мог бы сам сделать себя продавцом.
     */
    private void validateNotOwnApplication(SellerApplication application) {
        if (application.getKeycloakUserId().equals(currentUserService.getUserId())) {
            throw new ResourceForbiddenException(MessageKeys.OWN_APPLICATION_REVIEW_FORBIDDEN);
        }
    }

    private void validatePending(SellerApplication application) {
        if (application.getSellerApplicationStatus() != SellerApplicationStatus.PENDING) {
            throw new ResourceConflictException(MessageKeys.APPLICATION_ALREADY_REVIEWED, application.getId());
        }
    }

    private void validateSellerNotExist(String userId) {
        if (sellerRepo.existsByKeycloakUserIdOrApplicantKeycloakUserId(userId, userId)) {
            throw new ResourceConflictException(MessageKeys.SELLER_ALREADY_EXIST, userId);
        }
    }

    private void validateSellerNameUnique(String sellerName) {
        if (sellerRepo.existsSameName(sellerName)) {
            throw new ResourceConflictException(MessageKeys.SELLER_NAME_ALREADY_EXISTS, sellerName);
        }
    }

    /**
     * Сохраняет Seller сразу (flush) до gRPC-вызова, чтобы нарушение уникальности имени
     * при гонке стало 409, а роль в Keycloak не назначалась.
     */
    private void saveNewSeller(Seller seller) {
        try {
            sellerRepo.saveAndFlush(seller);
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolations.isUniqueViolation(e, SELLER_NAME_UNIQUE_INDEX, SELLER_NAME_HIBERNATE_CONSTRAINT)) {
                throw new ResourceConflictException(MessageKeys.SELLER_NAME_ALREADY_EXISTS, seller.getSellerName());
            }
            throw e;
        }
    }
}
