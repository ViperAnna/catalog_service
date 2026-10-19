package ru.klimovich.user_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.klimovich.user_service.repository.WishlistRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Change product-deletion-sync (D4): PRODUCT_DELETED удаляет id товара из всех вишлистов.
 */
@ExtendWith(MockitoExtension.class)
class ProductEventConsumerTest {

    @Mock
    private WishlistRepository wishlistRepo;

    private ProductEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ProductEventConsumer(wishlistRepo, new ObjectMapper());
    }

    private static String envelope(String eventType, String productId) {
        String payload = "{\\\"eventId\\\":\\\"ev-1\\\",\\\"occurredAt\\\":\\\"2026-10-15T12:00:00Z\\\","
                + "\\\"productId\\\":\\\"" + productId + "\\\",\\\"storeId\\\":3}";
        return "{\"aggregateType\":\"PRODUCT\",\"eventType\":\"" + eventType + "\",\"payload\":\"" + payload + "\"}";
    }

    @Test
    @DisplayName("PRODUCT_DELETED: id удаляется из всех вишлистов; повтор обрабатывается без ошибки")
    void productDeletedRemovesFromAllWishlists() {
        consumer.consume(envelope("PRODUCT_DELETED", "p-1"));
        consumer.consume(envelope("PRODUCT_DELETED", "p-1"));

        verify(wishlistRepo, times(2)).deleteProductFromAllWishlists("p-1");
    }

    @Test
    @DisplayName("Неизвестный eventType: пропускается без изменений")
    void unknownEventTypeSkipped() {
        consumer.consume(envelope("PRODUCT_ARCHIVED", "p-1"));

        verifyNoInteractions(wishlistRepo);
    }

    @Test
    @DisplayName("Невалидный JSON: исключение (повторы и DLT)")
    void malformedMessageThrows() {
        assertThatThrownBy(() -> consumer.consume("not-a-json")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(wishlistRepo);
    }

    @Test
    @DisplayName("PRODUCT_DELETED без productId: исключение (повторы и DLT)")
    void missingProductIdThrows() {
        String message = "{\"aggregateType\":\"PRODUCT\",\"eventType\":\"PRODUCT_DELETED\",\"payload\":\"{}\"}";

        assertThatThrownBy(() -> consumer.consume(message)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(wishlistRepo);
    }
}
