package ru.seller_service.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceForbiddenException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.mapper.StoreMapperImpl;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.model.StoreChangeRequest;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.model.status.StoreChangeRequestStatus;
import ru.seller_service.model.status.StoreStatus;
import ru.seller_service.repository.StoreChangeRequestRepository;
import ru.seller_service.repository.StoreRepository;
import ru.seller_service.security.CurrentUserService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Requirements "Store change request moderation", "Store change approval checks"
 * (change store-change-moderation).
 */
@ExtendWith(MockitoExtension.class)
class StoreChangeModerationServiceImplTest {

    private static final String OWNER = "kc-seller-1";
    private static final String MODERATOR = "kc-moderator-1";

    @Mock private StoreChangeRequestRepository changeRequestRepo;
    @Mock private StoreRepository storeRepo;
    @Mock private CurrentUserService currentUserService;

    private StoreChangeModerationServiceImpl service;
    private Seller seller;
    private Store store;

    @BeforeEach
    void setUp() {
        service = new StoreChangeModerationServiceImpl(changeRequestRepo, storeRepo, new StoreMapperImpl(),
                currentUserService);
        seller = new Seller();
        seller.setId(7L);
        seller.setSellerStatus(SellerStatus.ACTIVE);
        store = Store.builder().id(3L).seller(seller).storeName("Shop").description("descr")
                .email("shop@example.com").phoneNumber("+79990000000").status(StoreStatus.ACTIVE).build();
        lenient().when(currentUserService.getUserId()).thenReturn(MODERATOR);
    }

    private StoreChangeRequest request(StoreChangeRequestStatus status) {
        return StoreChangeRequest.builder().id(11L).store(store).keycloakUserId(OWNER)
                .storeName("New").description(null).email(null).phoneNumber("+79991112233")
                .status(status).build();
    }

    private void stubFound(StoreChangeRequest r) {
        when(changeRequestRepo.findById(11L)).thenReturn(Optional.of(r));
    }

    @Test
    @DisplayName("Очередь: PENDING по возрастанию даты")
    void pending() {
        when(changeRequestRepo.findAllByStatusOrderByCreatedAtAsc(StoreChangeRequestStatus.PENDING))
                .thenReturn(List.of(request(StoreChangeRequestStatus.PENDING)));

        List<StoreChangeRequestResponse> result = service.getPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStoreId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Одобрение: непустые поля переносятся в магазин, null не меняет, заявка APPROVED")
    void approve_appliesNonNullFields() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        stubFound(r);
        when(storeRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(store));
        when(storeRepo.saveAndFlush(store)).thenReturn(store);

        service.approve(11L);

        assertThat(store.getStoreName()).isEqualTo("New");
        assertThat(store.getPhoneNumber()).isEqualTo("+79991112233");
        assertThat(store.getDescription()).isEqualTo("descr");
        assertThat(store.getEmail()).isEqualTo("shop@example.com");
        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.APPROVED);
        verify(storeRepo).existsSameName(7L, "New", 3L);
    }

    @Test
    @DisplayName("Одобрение: имя занято за время рассмотрения -> 409, заявка PENDING, магазин не меняется")
    void approve_nameTaken() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        stubFound(r);
        when(storeRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(store));
        when(storeRepo.existsSameName(7L, "New", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.approve(11L)).isInstanceOf(ResourceConflictException.class);
        assertThat(store.getStoreName()).isEqualTo("Shop");
        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
        verify(storeRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Одобрение: нарушение индекса имени при сохранении -> 409, заявка PENDING")
    void approve_nameIndexViolation() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        stubFound(r);
        when(storeRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(store));
        when(storeRepo.saveAndFlush(store)).thenThrow(new DataIntegrityViolationException("dup",
                new SQLException("duplicate key value violates unique constraint \"uq_stores_seller_store_name\"", "23505")));

        assertThatThrownBy(() -> service.approve(11L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with name [New] already exists.");
        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
    }

    @Test
    @DisplayName("Одобрение: магазин заблокирован после подачи -> 409, заявка PENDING")
    void approve_storeBlocked() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        store.setStatus(StoreStatus.BLOCKED);
        stubFound(r);
        when(storeRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.approve(11L)).isInstanceOf(ResourceConflictException.class);
        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
        assertThat(store.getStoreName()).isEqualTo("Shop");
    }

    @Test
    @DisplayName("Одобрение: продавец заблокирован после подачи -> 409, заявка PENDING")
    void approve_sellerBlocked() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        seller.setSellerStatus(SellerStatus.BLOCKED);
        stubFound(r);
        when(storeRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.approve(11L)).isInstanceOf(ResourceConflictException.class);
        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
    }

    @Test
    @DisplayName("Одобрение: магазин удален одновременно -> 404 без изменений")
    void approve_storeDeletedConcurrently() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        stubFound(r);
        when(storeRepo.findByIdForUpdate(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(11L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
        verify(changeRequestRepo, never()).save(any());
    }

    @Test
    @DisplayName("Своя заявка: 403")
    void approve_ownRequest_forbidden() {
        when(currentUserService.getUserId()).thenReturn(OWNER);
        stubFound(request(StoreChangeRequestStatus.PENDING));

        assertThatThrownBy(() -> service.approve(11L)).isInstanceOf(ResourceForbiddenException.class);
        verify(storeRepo, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Заявка не PENDING: 409")
    void approve_alreadyReviewed() {
        stubFound(request(StoreChangeRequestStatus.REJECTED));

        assertThatThrownBy(() -> service.approve(11L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store change request with id: [11] already reviewed.");
    }

    @Test
    @DisplayName("Заявка не найдена: 404")
    void approve_notFound() {
        when(changeRequestRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Отклонение: REJECTED с причиной, магазин не меняется; работает для заблокированного магазина")
    void reject_blockedStoreAllowed() {
        StoreChangeRequest r = request(StoreChangeRequestStatus.PENDING);
        store.setStatus(StoreStatus.BLOCKED);
        stubFound(r);

        service.reject(11L, new ReviewSellerApplicationRequest("Invalid phone"));

        assertThat(r.getStatus()).isEqualTo(StoreChangeRequestStatus.REJECTED);
        assertThat(r.getModeratorComment()).isEqualTo("Invalid phone");
        assertThat(store.getStoreName()).isEqualTo("Shop");
        verify(storeRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Отклонение своей заявки: 403")
    void reject_ownRequest_forbidden() {
        when(currentUserService.getUserId()).thenReturn(OWNER);
        stubFound(request(StoreChangeRequestStatus.PENDING));

        assertThatThrownBy(() -> service.reject(11L, new ReviewSellerApplicationRequest("No")))
                .isInstanceOf(ResourceForbiddenException.class);
    }
}
