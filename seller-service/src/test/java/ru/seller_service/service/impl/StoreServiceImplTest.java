package ru.seller_service.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.request.store.StoreUpdateRequest;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.mapper.StoreMapperImpl;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.repository.StoreRepository;
import ru.seller_service.security.CurrentUserService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.outbox.StoreEventWriter;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.model.StoreChangeRequest;
import ru.seller_service.model.status.StoreChangeRequestStatus;
import ru.seller_service.repository.StoreChangeRequestRepository;
import java.util.List;
import ru.seller_service.model.status.StoreStatus;
import ru.seller_service.exception.ResourceConflictException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Requirements "Store phone normalization", "Store creation", "Store update"
 * (change fix-store-create-validation).
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

    private static final String USER_ID = "kc-seller-1";

    @Mock
    private StoreRepository storeRepo;
    @Mock
    private SellerRepository sellerRepo;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private OutboxRepository outboxRepo;
    @Mock
    private StoreChangeRequestRepository changeRequestRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StoreServiceImpl service;
    private Seller seller;

    @BeforeEach
    void setUp() {
        service = new StoreServiceImpl(storeRepo, new StoreMapperImpl(), sellerRepo, currentUserService,
                new StoreEventWriter(outboxRepo, new EventMapper(objectMapper)), changeRequestRepo);
        seller = new Seller();
        seller.setId(7L);
        seller.setKeycloakUserId(USER_ID);
        seller.setSellerStatus(SellerStatus.ACTIVE);
        when(currentUserService.getUserId()).thenReturn(USER_ID);
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller));
    }

    private Store existingStore() {
        return Store.builder()
                .id(3L)
                .seller(seller)
                .storeName("Shop")
                .description("descr")
                .email("shop@example.com")
                .phoneNumber("+79990000000")
                .build();
    }

    @Test
    @DisplayName("Store phone normalization: создание с 8XXXXXXXXXX сохраняет +7XXXXXXXXXX")
    void createStore_eightPrefix_savedNormalized() {
        when(storeRepo.saveAndFlush(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "89991234567"));

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepo).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("+79991234567");
        assertThat(response.getPhoneNumber()).isEqualTo("+79991234567");
    }

    // --- Store update = заявка на изменение (change store-change-moderation) ---

    private StoreChangeRequest capturedRequest() {
        ArgumentCaptor<StoreChangeRequest> captor = ArgumentCaptor.forClass(StoreChangeRequest.class);
        verify(changeRequestRepo).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void stubRequestSave() {
        when(changeRequestRepo.saveAndFlush(any(StoreChangeRequest.class))).thenAnswer(invocation -> {
            StoreChangeRequest r = invocation.getArgument(0);
            r.setId(11L);
            return r;
        });
    }

    @Test
    @DisplayName("Store phone normalization: заявка сохраняет +7XXXXXXXXXX, магазин не меняется")
    void updateStore_eightPrefix_savedNormalized() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));
        stubRequestSave();

        service.updateStore(3L, new StoreUpdateRequest("Shop New", null, null, "89990001122"));

        assertThat(capturedRequest().getPhoneNumber()).isEqualTo("+79990001122");
        assertThat(store.getPhoneNumber()).isEqualTo("+79990000000");
        assertThat(store.getStoreName()).isEqualTo("Shop");
        verify(storeRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Store update: без email и телефона в заявке null, магазин не меняется")
    void updateStore_withoutEmailAndPhone_keepsValues() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));
        stubRequestSave();

        service.updateStore(3L, new StoreUpdateRequest("Shop New", null, null, null));

        StoreChangeRequest request = capturedRequest();
        assertThat(request.getEmail()).isNull();
        assertThat(request.getPhoneNumber()).isNull();
        assertThat(request.getStoreName()).isEqualTo("Shop New");
        assertThat(store.getEmail()).isEqualTo("shop@example.com");
    }

    @Test
    @DisplayName("Store update: PENDING-заявка с автором и магазином, ответ с заявкой")
    void updateStore_createsPendingRequest() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));
        stubRequestSave();

        StoreChangeRequestResponse response =
                service.updateStore(3L, new StoreUpdateRequest("Shop", "descr 2", "new@example.com", null));

        StoreChangeRequest request = capturedRequest();
        assertThat(request.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
        assertThat(request.getStore()).isSameAs(store);
        assertThat(request.getKeycloakUserId()).isEqualTo(USER_ID);
        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getStoreId()).isEqualTo(3L);
        assertThat(response.getStatus()).isEqualTo(StoreChangeRequestStatus.PENDING);
        assertThat(response.getDescription()).isEqualTo("descr 2");
    }

    @Test
    @DisplayName("Store update: вторая заявка при PENDING -> 409, заявка не создается")
    void updateStore_pendingExists_conflict() {
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(existingStore()));
        when(changeRequestRepo.existsByStoreIdAndStatus(3L, StoreChangeRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.updateStore(3L, new StoreUpdateRequest("Shop", null, null, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with id: [3] already has a pending change request.");
        verify(changeRequestRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Store update: одновременная подача -> индекс uq_store_change_requests_pending -> 409")
    void updateStore_pendingIndexViolation_conflict() {
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(existingStore()));
        when(changeRequestRepo.saveAndFlush(any(StoreChangeRequest.class))).thenThrow(indexViolation("23505",
                "ERROR: duplicate key value violates unique constraint \"uq_store_change_requests_pending\""));

        assertThatThrownBy(() -> service.updateStore(3L, new StoreUpdateRequest("Shop", null, null, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with id: [3] already has a pending change request.");
    }

    @Test
    @DisplayName("Store update: чужой или удаленный магазин -> 404")
    void updateStore_foreignStore_notFound() {
        when(storeRepo.findByIdAndSellerIdForUpdate(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStore(99L, new StoreUpdateRequest("Shop", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(changeRequestRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Store change requests retrieval: заявки по убыванию даты, 404 для чужого магазина")
    void getChangeRequests() {
        Store store = existingStore();
        StoreChangeRequest pending = StoreChangeRequest.builder().id(12L).store(store)
                .status(StoreChangeRequestStatus.PENDING).build();
        StoreChangeRequest rejected = StoreChangeRequest.builder().id(10L).store(store)
                .status(StoreChangeRequestStatus.REJECTED).moderatorComment("No").build();
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));
        when(changeRequestRepo.findAllByStoreIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(pending, rejected));
        when(storeRepo.findByIdAndSellerId(99L, 7L)).thenReturn(Optional.empty());

        List<StoreChangeRequestResponse> result = service.getChangeRequests(3L);

        assertThat(result).extracting(StoreChangeRequestResponse::getId).containsExactly(12L, 10L);
        assertThat(result.get(1).getModeratorComment()).isEqualTo("No");
        assertThatThrownBy(() -> service.getChangeRequests(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Store name uniqueness per seller (change store-name-unique-per-seller) ---

    private static DataIntegrityViolationException indexViolation(String sqlState, String message) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLException(message, sqlState));
    }

    @Test
    @DisplayName("Store creation: storeName сохраняется без пробелов по краям, проверка без excludeId")
    void createStore_trimsNameAndChecksWithoutExclusion() {
        when(storeRepo.saveAndFlush(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = service.createStore(
                new StoreRequest("  My Shop  ", null, "shop@example.com", "+79991234567"));

        verify(storeRepo).existsSameName(7L, "My Shop", null);
        assertThat(response.getStoreName()).isEqualTo("My Shop");
    }

    @Test
    @DisplayName("Store name uniqueness per seller: подача исключает сам магазин из проверки (OBS-05)")
    void updateStore_checksExcludingItself() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));
        stubRequestSave();

        service.updateStore(3L, new StoreUpdateRequest(" shop ", "new descr", null, null));

        verify(storeRepo).existsSameName(7L, "shop", 3L);
        StoreChangeRequest request = capturedRequest();
        assertThat(request.getStoreName()).isEqualTo("shop");
        assertThat(request.getDescription()).isEqualTo("new descr");
        assertThat(store.getDescription()).isEqualTo("descr");
    }

    @Test
    @DisplayName("Store name uniqueness per seller: имя другого своего магазина в заявке -> 409")
    void updateStore_nameTaken_conflict() {
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(existingStore()));
        when(storeRepo.existsSameName(7L, "shop", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateStore(3L, new StoreUpdateRequest(" shop ", null, null, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with name [shop] already exists.");
        verify(changeRequestRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Store name uniqueness per seller: занятое имя -> 409 STORE_ALREADY_EXISTS")
    void createStore_nameTaken_conflict() {
        when(storeRepo.existsSameName(7L, "Shop", null)).thenReturn(true);

        assertThatThrownBy(() -> service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "+79991234567")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with name [Shop] already exists.");
        verify(storeRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Store name uniqueness per seller: нарушение индекса при гонке -> 409")
    void createStore_uniqueIndexViolation_conflict() {
        when(storeRepo.saveAndFlush(any(Store.class))).thenThrow(indexViolation("23505",
                "ERROR: duplicate key value violates unique constraint \"uq_stores_seller_store_name\""));

        assertThatThrownBy(() -> service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "+79991234567")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with name [Shop] already exists.");
    }

    @Test
    @DisplayName("Store name uniqueness per seller: другой SQLState пробрасывается без изменений")
    void createStore_otherIntegrityViolation_rethrown() {
        DataIntegrityViolationException notNull = indexViolation("23502",
                "ERROR: null value in column \"email\" violates not-null constraint");
        when(storeRepo.saveAndFlush(any(Store.class))).thenThrow(notNull);

        assertThatThrownBy(() -> service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "+79991234567")))
                .isSameAs(notNull);
    }

    @Test
    @DisplayName("Store name uniqueness per seller: 23505 другого ограничения пробрасывается без изменений")
    void createStore_otherUniqueViolation_rethrown() {
        DataIntegrityViolationException otherUnique = indexViolation("23505",
                "ERROR: duplicate key value violates unique constraint \"stores_pkey\"");
        when(storeRepo.saveAndFlush(any(Store.class))).thenThrow(otherUnique);

        assertThatThrownBy(() -> service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "+79991234567")))
                .isSameAs(otherUnique);
    }

    // --- Store deleted event (change archive-products-of-deleted-store) ---

    @Test
    @DisplayName("Store deleted event: удаление магазина пишет STORE_PURGED в outbox (change seller-store-lifecycle)")
    void deleteStore_writesStorePurgedOutboxEvent() throws Exception {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));

        service.deleteStore(3L);

        verify(storeRepo).delete(store);
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("STORE");
        assertThat(event.getAggregateId()).isEqualTo("3");
        assertThat(event.getEventType()).isEqualTo("STORE_PURGED");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getCreatedAt()).isNotNull();
        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("storeId").asLong()).isEqualTo(3L);
        assertThat(payload.get("sellerId").asLong()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Store deleted event: чужой магазин -> 404, событие не пишется")
    void deleteStore_foreignStore_noEvent() {
        when(storeRepo.findByIdAndSellerIdForUpdate(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteStore(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(outboxRepo);
        verify(storeRepo, never()).delete(any());
    }
    // --- Store status и ограничения по статусу (change seller-store-lifecycle) ---

    @Test
    @DisplayName("Store status: новый магазин ACTIVE, ответ содержит status и sellerStatus")
    void createStore_activeStatusInResponse() {
        when(storeRepo.saveAndFlush(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StoreResponse response = service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "+79991234567"));

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
        verify(storeRepo).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(response.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(response.getSellerStatus()).isEqualTo(SellerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Blocked seller restrictions: заблокированный продавец не создает магазин")
    void createStore_blockedSeller_conflict() {
        seller.setSellerStatus(SellerStatus.BLOCKED);

        assertThatThrownBy(() -> service.createStore(
                new StoreRequest("Shop", null, "shop@example.com", "+79991234567")))
                .isInstanceOf(ResourceConflictException.class);
        verify(storeRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Blocked seller restrictions: заблокированный продавец не изменяет магазин")
    void updateStore_blockedSeller_conflict() {
        seller.setSellerStatus(SellerStatus.BLOCKED);
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(existingStore()));

        assertThatThrownBy(() -> service.updateStore(3L, new StoreUpdateRequest("New", null, null, null)))
                .isInstanceOf(ResourceConflictException.class);
        verify(storeRepo, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Blocked seller restrictions: заблокированный продавец не удаляет магазин")
    void deleteStore_blockedSeller_conflict() {
        seller.setSellerStatus(SellerStatus.BLOCKED);
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(existingStore()));

        assertThatThrownBy(() -> service.deleteStore(3L)).isInstanceOf(ResourceConflictException.class);
        verify(storeRepo, never()).delete(any());
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Blocked store restrictions: заблокированный магазин не изменяется")
    void updateStore_blockedStore_conflict() {
        Store store = existingStore();
        store.setStatus(StoreStatus.BLOCKED);
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.updateStore(3L, new StoreUpdateRequest("New", null, null, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with id: [3] is blocked.");
        assertThat(store.getStoreName()).isEqualTo("Shop");
    }

    @Test
    @DisplayName("Blocked store restrictions: заблокированный магазин не удаляется, события нет")
    void deleteStore_blockedStore_conflict() {
        Store store = existingStore();
        store.setStatus(StoreStatus.BLOCKED);
        when(storeRepo.findByIdAndSellerIdForUpdate(3L, 7L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.deleteStore(3L)).isInstanceOf(ResourceConflictException.class);
        verify(storeRepo, never()).delete(any());
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Просмотр: заблокированный продавец видит свой магазин со статусами")
    void getStore_blockedSeller_allowed() {
        seller.setSellerStatus(SellerStatus.BLOCKED);
        Store store = existingStore();
        store.setStatus(StoreStatus.BLOCKED);
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));

        StoreResponse response = service.getStoreById(3L);

        assertThat(response.getStatus()).isEqualTo(StoreStatus.BLOCKED);
        assertThat(response.getSellerStatus()).isEqualTo(SellerStatus.BLOCKED);
    }
}
