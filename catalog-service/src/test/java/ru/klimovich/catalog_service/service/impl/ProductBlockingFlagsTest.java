package ru.klimovich.catalog_service.service.impl;

import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import ru.klimovich.catalog_service.model.Product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Requirement "Product blocking flags" (change seller-store-lifecycle): абсолютное значение
 * одного признака для всех товаров магазина, другой признак и статус не меняются.
 */
@ExtendWith(MockitoExtension.class)
class ProductBlockingFlagsTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductBlockingFlags flags;

    private Document capturedSet(Query[] queryHolder) {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq(Product.class));
        queryHolder[0] = query.getValue();
        assertThat(update.getValue().getUpdateObject()).containsOnlyKeys("$set");
        return (Document) update.getValue().getUpdateObject().get("$set");
    }

    @Test
    @DisplayName("STORE_BLOCKED: storeBlocked = true для всех товаров магазина")
    void storeBlocked() {
        when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Product.class)))
                .thenReturn(UpdateResult.acknowledged(2, 2L, null));

        flags.setStoreBlocked(3L, true);

        Query[] q = new Query[1];
        Document set = capturedSet(q);
        assertThat(q[0].getQueryObject()).isEqualTo(new Document("storeId", 3L));
        assertThat(set).containsOnlyKeys("storeBlocked");
        assertThat(set.get("storeBlocked")).isEqualTo(true);
    }

    @Test
    @DisplayName("SELLER_UNBLOCKED: sellerBlocked = false, storeBlocked не затрагивается")
    void sellerUnblocked() {
        when(mongoTemplate.updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Product.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));

        flags.setSellerBlocked(3L, false);

        Document set = capturedSet(new Query[1]);
        assertThat(set).containsOnlyKeys("sellerBlocked");
        assertThat(set.get("sellerBlocked")).isEqualTo(false);
    }
}
