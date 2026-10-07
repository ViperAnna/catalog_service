package ru.seller_service.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.model.Seller;
import ru.seller_service.model.Store;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requirement "Store response format" (openspec/specs/seller-stores/spec.md,
 * change fix-store-response-fields).
 */
class StoreMapperTest {

    private final StoreMapper mapper = new StoreMapperImpl();

    private static Store store(Seller seller) {
        return Store.builder()
                .id(3L)
                .seller(seller)
                .storeName("Shop")
                .description("descr")
                .email("shop@example.com")
                .phoneNumber("+79990000000")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();
    }

    private static Seller seller(Long id) {
        Seller seller = new Seller();
        seller.setId(id);
        return seller;
    }

    @Test
    @DisplayName("Store response format: sellerId и phoneNumber заполнены")
    void toDTO_fillsSellerIdAndPhoneNumber() {
        StoreResponse response = mapper.toDTO(store(seller(7L)));

        assertThat(response.getSellerId()).isEqualTo(7L);
        assertThat(response.getPhoneNumber()).isEqualTo("+79990000000");
    }

    @Test
    @DisplayName("Store response format: остальные поля маппятся без изменений")
    void toDTO_mapsOtherFields() {
        StoreResponse response = mapper.toDTO(store(seller(7L)));

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getStoreName()).isEqualTo("Shop");
        assertThat(response.getDescription()).isEqualTo("descr");
        assertThat(response.getEmail()).isEqualTo("shop@example.com");
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 12, 0));
    }

    @Test
    @DisplayName("Store response format: магазин без продавца дает sellerId = null без исключения")
    void toDTO_withoutSeller_sellerIdNull() {
        StoreResponse response = mapper.toDTO(store(null));

        assertThat(response.getSellerId()).isNull();
        assertThat(response.getPhoneNumber()).isEqualTo("+79990000000");
        assertThat(response.getStoreName()).isEqualTo("Shop");
    }
}
