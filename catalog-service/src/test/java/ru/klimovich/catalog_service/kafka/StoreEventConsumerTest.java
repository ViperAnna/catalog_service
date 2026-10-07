package ru.klimovich.catalog_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.klimovich.catalog_service.service.impl.StoreProductsArchiver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Requirements "Archiving products of deleted store" и
 * "Store events consumer error handling" (catalog-products,
 * change archive-products-of-deleted-store).
 */
@ExtendWith(MockitoExtension.class)
class StoreEventConsumerTest {

    @Mock
    private StoreProductsArchiver archiver;

    private StoreEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new StoreEventConsumer(archiver, new ObjectMapper());
    }

    @Test
    @DisplayName("STORE_DELETED архивирует товары магазина из payload")
    void storeDeleted_archivesProductsOfStore() {
        String message = "{\"aggregateType\":\"STORE\",\"eventType\":\"STORE_DELETED\","
                + "\"payload\":\"{\\\"storeId\\\":3,\\\"sellerId\\\":7}\"}";

        consumer.consume(message);

        verify(archiver).archiveProductsOfStore(3L);
    }

    @Test
    @DisplayName("Неизвестный eventType пропускается без исключения")
    void unknownEventType_skipped() {
        String message = "{\"aggregateType\":\"STORE\",\"eventType\":\"STORE_RENAMED\",\"payload\":\"{}\"}";

        consumer.consume(message);

        verify(archiver, never()).archiveProductsOfStore(anyLong());
    }

    @Test
    @DisplayName("Некорректный JSON бросает исключение для обработчика ошибок (повторы и DLT)")
    void malformedJson_throws() {
        assertThatThrownBy(() -> consumer.consume("not-a-json"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(archiver, never()).archiveProductsOfStore(anyLong());
    }

    @Test
    @DisplayName("STORE_DELETED без storeId в payload бросает исключение")
    void storeDeletedWithoutStoreId_throws() {
        String message = "{\"aggregateType\":\"STORE\",\"eventType\":\"STORE_DELETED\",\"payload\":\"{}\"}";

        assertThatThrownBy(() -> consumer.consume(message))
                .isInstanceOf(IllegalArgumentException.class);
        verify(archiver, never()).archiveProductsOfStore(anyLong());
    }
}
