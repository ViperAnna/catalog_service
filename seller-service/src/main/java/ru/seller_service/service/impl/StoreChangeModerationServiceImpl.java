package ru.seller_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceForbiddenException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.mapper.StoreMapper;
import ru.seller_service.model.Store;
import ru.seller_service.model.StoreChangeRequest;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.model.status.StoreChangeRequestStatus;
import ru.seller_service.model.status.StoreStatus;
import ru.seller_service.repository.StoreChangeRequestRepository;
import ru.seller_service.repository.StoreRepository;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.service.StoreChangeModerationService;
import ru.seller_service.util.MessageKeys;
import ru.seller_service.util.UniqueViolations;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Правила как у заявок продавца: рассматривается только PENDING, свою заявку модератор не
 * рассматривает. Одобрение проверяет текущие статусы и уникальность имени; при отказе по этим
 * причинам заявка остается PENDING.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StoreChangeModerationServiceImpl implements StoreChangeModerationService {

    private static final String STORE_NAME_UNIQUE_INDEX = "uq_stores_seller_store_name";

    private final StoreChangeRequestRepository changeRequestRepo;
    private final StoreRepository storeRepo;
    private final StoreMapper storeMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<StoreChangeRequestResponse> getPending() {
        return changeRequestRepo.findAllByStatusOrderByCreatedAtAsc(StoreChangeRequestStatus.PENDING)
                .stream()
                .map(storeMapper::toChangeRequestDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreChangeRequestResponse getRequest(Long requestId) {
        return storeMapper.toChangeRequestDTO(getRequestById(requestId));
    }

    @Override
    public void approve(Long requestId) {
        StoreChangeRequest request = getReviewableRequest(requestId);

        // Блокировка строки магазина: одновременное удаление ждет или уже выполнено (тогда 404).
        Store store = storeRepo.findByIdForUpdate(request.getStore().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageKeys.STORE_NOT_FOUND, String.valueOf(request.getStore().getId())));
        if (store.getSeller().getSellerStatus() == SellerStatus.BLOCKED) {
            throw new ResourceConflictException(MessageKeys.SELLER_IS_NOT_ACTIVE, store.getSeller().getSellerStatus().name());
        }
        if (store.getStatus() == StoreStatus.BLOCKED) {
            throw new ResourceConflictException(MessageKeys.STORE_IS_BLOCKED, store.getId());
        }
        if (request.getStoreName() != null
                && storeRepo.existsSameName(store.getSeller().getId(), request.getStoreName(), store.getId())) {
            throw new ResourceConflictException(MessageKeys.STORE_ALREADY_EXISTS, request.getStoreName());
        }

        applyNonNullFields(request, store);
        saveStore(store);

        request.setStatus(StoreChangeRequestStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());
        changeRequestRepo.save(request);
    }

    @Override
    public void reject(Long requestId, ReviewSellerApplicationRequest review) {
        StoreChangeRequest request = getReviewableRequest(requestId);

        request.setStatus(StoreChangeRequestStatus.REJECTED);
        request.setModeratorComment(review.moderatorComment());
        request.setUpdatedAt(LocalDateTime.now());
        changeRequestRepo.save(request);
    }

    private StoreChangeRequest getReviewableRequest(Long requestId) {
        StoreChangeRequest request = getRequestById(requestId);
        if (request.getKeycloakUserId().equals(currentUserService.getUserId())) {
            throw new ResourceForbiddenException(MessageKeys.OWN_APPLICATION_REVIEW_FORBIDDEN);
        }
        if (request.getStatus() != StoreChangeRequestStatus.PENDING) {
            throw new ResourceConflictException(MessageKeys.STORE_CHANGE_REQUEST_ALREADY_REVIEWED, requestId);
        }
        return request;
    }

    private StoreChangeRequest getRequestById(Long requestId) {
        return changeRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageKeys.STORE_CHANGE_REQUEST_NOT_FOUND, String.valueOf(requestId)));
    }

    /** null в заявке означает «не менять текущее значение». */
    private static void applyNonNullFields(StoreChangeRequest request, Store store) {
        if (request.getStoreName() != null) {
            store.setStoreName(request.getStoreName());
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription());
        }
        if (request.getEmail() != null) {
            store.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            store.setPhoneNumber(request.getPhoneNumber());
        }
        store.setUpdatedAt(LocalDateTime.now());
    }

    /** Индекс имени — последняя защита от дубликата, если проверка выше прошла в гонке. */
    private void saveStore(Store store) {
        try {
            storeRepo.saveAndFlush(store);
        } catch (DataIntegrityViolationException e) {
            if (UniqueViolations.isUniqueViolation(e, STORE_NAME_UNIQUE_INDEX)) {
                throw new ResourceConflictException(MessageKeys.STORE_ALREADY_EXISTS, store.getStoreName());
            }
            throw e;
        }
    }
}
