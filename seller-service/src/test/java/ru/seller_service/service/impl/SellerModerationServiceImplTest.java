package ru.seller_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.model.status.StoreStatus;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.outbox.StoreEventWriter;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.repository.StoreRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Requirements "Seller blocking by moderator", "Store blocking by moderator",
 * "Store availability events" (change seller-store-lifecycle).
 */
@ExtendWith(MockitoExtension.class)
class SellerModerationServiceImplTest {

    @Mock
    private SellerRepository sellerRepo;
    @Mock
    private StoreRepository storeRepo;
    @Mock
    private OutboxRepository outboxRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SellerModerationServiceImpl service;
    private Seller seller;

    @BeforeEach
    void setUp() {
        service = new SellerModerationServiceImpl(sellerRepo, storeRepo,
                new StoreEventWriter(outboxRepo, new EventMapper(objectMapper)));
        seller = new Seller();
        seller.setId(7L);
        seller.setSellerStatus(SellerStatus.ACTIVE);
    }

    private Store store(Long id, StoreStatus status) {
        return Store.builder().id(id).seller(seller).storeName("Shop " + id).status(status).build();
    }

    private List<OutboxEvent> savedEvents(int count) {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo, times(count)).save(captor.capture());
        return captor.getAllValues();
    }

    private void assertStoreEvent(OutboxEvent event, String type, long storeId) throws Exception {
        assertThat(event.getAggregateType()).isEqualTo("STORE");
        assertThat(event.getEventType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(String.valueOf(storeId));
        assertThat(event.isProcessed()).isFalse();
        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("storeId").asLong()).isEqualTo(storeId);
        assertThat(payload.get("sellerId").asLong()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Блокировка продавца: BLOCKED, SELLER_BLOCKED на каждый магазин, магазины не меняются")
    void blockSeller() throws Exception {
        Store s3 = store(3L, StoreStatus.ACTIVE);
        Store s4 = store(4L, StoreStatus.BLOCKED);
        when(sellerRepo.findById(7L)).thenReturn(Optional.of(seller));
        when(storeRepo.findAllBySellerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(s3, s4));

        service.blockSeller(7L);

        assertThat(seller.getSellerStatus()).isEqualTo(SellerStatus.BLOCKED);
        assertThat(s3.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(s4.getStatus()).isEqualTo(StoreStatus.BLOCKED);
        List<OutboxEvent> events = savedEvents(2);
        assertStoreEvent(events.get(0), "SELLER_BLOCKED", 3L);
        assertStoreEvent(events.get(1), "SELLER_BLOCKED", 4L);
        verify(storeRepo, never()).delete(any());
        verify(sellerRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Разблокировка продавца: ACTIVE, SELLER_UNBLOCKED, отдельно заблокированный магазин остается BLOCKED")
    void unblockSeller() throws Exception {
        seller.setSellerStatus(SellerStatus.BLOCKED);
        Store s3 = store(3L, StoreStatus.BLOCKED);
        when(sellerRepo.findById(7L)).thenReturn(Optional.of(seller));
        when(storeRepo.findAllBySellerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(s3));

        service.unblockSeller(7L);

        assertThat(seller.getSellerStatus()).isEqualTo(SellerStatus.ACTIVE);
        assertThat(s3.getStatus()).isEqualTo(StoreStatus.BLOCKED);
        assertStoreEvent(savedEvents(1).get(0), "SELLER_UNBLOCKED", 3L);
    }

    @Test
    @DisplayName("Повторная блокировка продавца: 409, событий нет")
    void blockSeller_alreadyBlocked_conflict() {
        seller.setSellerStatus(SellerStatus.BLOCKED);
        when(sellerRepo.findById(7L)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> service.blockSeller(7L)).isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Разблокировка ACTIVE-продавца: 409")
    void unblockSeller_active_conflict() {
        when(sellerRepo.findById(7L)).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> service.unblockSeller(7L)).isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Продавец не найден: 404")
    void blockSeller_notFound() {
        when(sellerRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.blockSeller(99L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Блокировка магазина: BLOCKED и STORE_BLOCKED")
    void blockStore() throws Exception {
        Store s3 = store(3L, StoreStatus.ACTIVE);
        when(storeRepo.findById(3L)).thenReturn(Optional.of(s3));

        service.blockStore(3L);

        assertThat(s3.getStatus()).isEqualTo(StoreStatus.BLOCKED);
        assertStoreEvent(savedEvents(1).get(0), "STORE_BLOCKED", 3L);
        verify(storeRepo, never()).delete(any());
    }

    @Test
    @DisplayName("Разблокировка магазина заблокированного продавца: ACTIVE и STORE_UNBLOCKED, продавец остается BLOCKED")
    void unblockStore_sellerBlocked() throws Exception {
        seller.setSellerStatus(SellerStatus.BLOCKED);
        Store s3 = store(3L, StoreStatus.BLOCKED);
        when(storeRepo.findById(3L)).thenReturn(Optional.of(s3));

        service.unblockStore(3L);

        assertThat(s3.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(seller.getSellerStatus()).isEqualTo(SellerStatus.BLOCKED);
        assertStoreEvent(savedEvents(1).get(0), "STORE_UNBLOCKED", 3L);
    }

    @Test
    @DisplayName("Разблокировка ACTIVE-магазина: 409, событий нет")
    void unblockStore_active_conflict() {
        when(storeRepo.findById(3L)).thenReturn(Optional.of(store(3L, StoreStatus.ACTIVE)));

        assertThatThrownBy(() -> service.unblockStore(3L)).isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Магазин не найден: 404")
    void blockStore_notFound() {
        when(storeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.blockStore(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    @DisplayName("Удаление продавца: STORE_PURGED на каждый магазин, статусы не проверяются")
    void writeStorePurgedEvents() throws Exception {
        when(storeRepo.findAllBySellerIdForUpdate(7L))
                .thenReturn(List.of(store(3L, StoreStatus.ACTIVE), store(4L, StoreStatus.BLOCKED)));

        service.writeStorePurgedEvents(7L);

        List<OutboxEvent> events = savedEvents(2);
        assertStoreEvent(events.get(0), "STORE_PURGED", 3L);
        assertStoreEvent(events.get(1), "STORE_PURGED", 4L);
    }
}
