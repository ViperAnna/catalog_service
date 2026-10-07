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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StoreServiceImpl service;
    private Seller seller;

    @BeforeEach
    void setUp() {
        service = new StoreServiceImpl(storeRepo, new StoreMapperImpl(), sellerRepo, currentUserService,
                outboxRepo, new EventMapper(objectMapper));
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

    @Test
    @DisplayName("Store phone normalization: изменение с 8XXXXXXXXXX сохраняет +7XXXXXXXXXX")
    void updateStore_eightPrefix_savedNormalized() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));
        when(storeRepo.saveAndFlush(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStore(3L, new StoreUpdateRequest("Shop New", null, null, "89990001122"));

        assertThat(store.getPhoneNumber()).isEqualTo("+79990001122");
    }

    @Test
    @DisplayName("Store update: без email и телефона прежние значения сохраняются")
    void updateStore_withoutEmailAndPhone_keepsValues() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));
        when(storeRepo.saveAndFlush(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStore(3L, new StoreUpdateRequest("Shop New", null, null, null));

        assertThat(store.getEmail()).isEqualTo("shop@example.com");
        assertThat(store.getPhoneNumber()).isEqualTo("+79990000000");
        assertThat(store.getStoreName()).isEqualTo("Shop New");
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
    @DisplayName("Store name uniqueness per seller: PUT исключает сам магазин из проверки (OBS-05)")
    void updateStore_checksExcludingItself() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));
        when(storeRepo.saveAndFlush(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStore(3L, new StoreUpdateRequest(" shop ", "new descr", null, null));

        verify(storeRepo).existsSameName(7L, "shop", 3L);
        assertThat(store.getStoreName()).isEqualTo("shop");
        assertThat(store.getDescription()).isEqualTo("new descr");
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
    @DisplayName("Store name uniqueness per seller: нарушение индекса при PUT -> 409")
    void updateStore_uniqueIndexViolation_conflict() {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));
        when(storeRepo.saveAndFlush(any(Store.class))).thenThrow(indexViolation("23505",
                "ERROR: duplicate key value violates unique constraint \"uq_stores_seller_store_name\""));

        assertThatThrownBy(() -> service.updateStore(3L, new StoreUpdateRequest("Outlet", null, null, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Store with name [Outlet] already exists.");
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
    @DisplayName("Store deleted event: удаление магазина пишет STORE_DELETED в outbox")
    void deleteStore_writesStoreDeletedOutboxEvent() throws Exception {
        Store store = existingStore();
        when(storeRepo.findByIdAndSellerId(3L, 7L)).thenReturn(Optional.of(store));

        service.deleteStore(3L);

        verify(storeRepo).delete(store);
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("STORE");
        assertThat(event.getAggregateId()).isEqualTo("3");
        assertThat(event.getEventType()).isEqualTo("STORE_DELETED");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getCreatedAt()).isNotNull();
        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("storeId").asLong()).isEqualTo(3L);
        assertThat(payload.get("sellerId").asLong()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Store deleted event: чужой магазин -> 404, событие не пишется")
    void deleteStore_foreignStore_noEvent() {
        when(storeRepo.findByIdAndSellerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteStore(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(outboxRepo);
        verify(storeRepo, never()).delete(any());
    }
}
